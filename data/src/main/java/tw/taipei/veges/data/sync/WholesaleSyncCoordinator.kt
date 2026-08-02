package tw.taipei.veges.data.sync

import java.time.Instant
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import tw.taipei.veges.data.local.SourceDayStateEntity
import tw.taipei.veges.data.local.SourceObservationEntity
import tw.taipei.veges.data.local.SyncRunEntity
import tw.taipei.veges.data.local.VegesDatabase
import tw.taipei.veges.data.network.MoaWholesaleClient
import tw.taipei.veges.data.network.MoaWholesaleHistoryQuery
import tw.taipei.veges.data.network.WholesaleValidation
import tw.taipei.veges.data.network.classifySourceDay
import tw.taipei.veges.data.network.validateWholesale
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.PriceRefreshStage
import tw.taipei.veges.domain.SourceDayState
import tw.taipei.veges.domain.SourceKind

sealed interface SyncResult {
    data class Published(
        val runId: String,
        val accepted: Int,
        val invalid: Int,
        val sourceDays: Int,
    ) : SyncResult

    data class Failed(val runId: String, val reason: String) : SyncResult
}

class WholesaleSyncCoordinator @Inject constructor(
    private val client: MoaWholesaleClient,
    private val database: VegesDatabase,
    private val clock: Clock,
) {
    suspend fun synchronizeLatest(
        onProgress: (PriceRefreshStage, Float) -> Unit = { _, _ -> },
    ): SyncResult = synchronize(
        fetchLatest = true,
        includeCatalogHistory = false,
        onProgress = onProgress,
    )

    suspend fun synchronizeCatalogHistory(
        onProgress: (PriceRefreshStage, Float) -> Unit = { _, _ -> },
    ): SyncResult = synchronize(
        fetchLatest = false,
        includeCatalogHistory = true,
        onProgress = onProgress,
    )

    suspend fun synchronizeConceptHistory(
        conceptId: String,
        onProgress: (PriceRefreshStage, Float) -> Unit = { _, _ -> },
    ): SyncResult = synchronize(
        requestedConceptId = conceptId,
        fetchLatest = false,
        includeCatalogHistory = false,
        onProgress = onProgress,
    )

    suspend fun synchronize(
        requestedFrom: LocalDate? = null,
        requestedTo: LocalDate? = null,
        requestedConceptId: String? = null,
        etag: String? = null,
        lastModified: String? = null,
        fetchLatest: Boolean = true,
        includeCatalogHistory: Boolean = true,
        onProgress: (PriceRefreshStage, Float) -> Unit = { _, _ -> },
    ): SyncResult {
        val runId = UUID.randomUUID().toString()
        val startedAt = Instant.now(clock)
        return try {
            val snapshot = if (fetchLatest) {
                onProgress(PriceRefreshStage.LATEST_PRICES, 0.12f)
                client.fetch(etag, lastModified).also {
                    onProgress(PriceRefreshStage.LATEST_PRICES, 0.80f)
                }
            } else {
                null
            }
            val today = LocalDate.now(clock)
            val allPublishedConcepts = database.taxonomyDao().publishedConceptsWithDetails()
            val allowedCodes = allPublishedConcepts
                .flatMap { it.variants }
                .map { it.commodityCode }
                .toSet()
            val requestedConcept = requestedConceptId?.let { conceptId ->
                requireNotNull(database.taxonomyDao().publishedConceptWithDetails(conceptId)) {
                    "Requested concept is not published: $conceptId"
                }
            }
            val requestedCodes = requestedConcept
                ?.variants
                ?.map { it.commodityCode }
                ?.distinct()
                .orEmpty()
            val earliestRequestedObservation = requestedCodes
                .takeIf(List<String>::isNotEmpty)
                ?.let { database.sourceDao().earliestValidWholesaleObservationDate(it) }
            val bootstrapFrom = today.minusDays(CATALOG_HISTORY_DAYS - 1L)
            val backfillFrom = requestedFrom ?: when {
                requestedConcept != null -> today.minusDays(HISTORY_DAYS - 1L)
                else -> bootstrapFrom
            }
            val backfillTo = requestedTo ?: today
            val needsRequestedHistory = requestedConcept != null &&
                (
                    earliestRequestedObservation == null ||
                        earliestRequestedObservation.isAfter(
                            backfillFrom.plusDays(BACKFILL_BOUNDARY_TOLERANCE_DAYS),
                        )
                    )
            val catalogHistoryQueries = if (includeCatalogHistory && requestedConcept == null) {
                listOf(
                    MarketBasis.TAIPEI_FIRST,
                    MarketBasis.TAIPEI_SECOND,
                ).flatMap { market ->
                    val completedDates = database.sourceDao().completedWholesaleSourceDates(
                        market = market,
                        from = backfillFrom,
                        to = backfillTo,
                    )
                    missingDateRanges(
                        from = backfillFrom,
                        to = backfillTo,
                        completedDates = completedDates,
                    ).map { range ->
                        MoaWholesaleHistoryQuery(
                            from = range.from,
                            to = range.to,
                            market = market,
                        )
                    }
                }
            } else {
                emptyList()
            }
            val historyQueries = when {
                needsRequestedHistory -> requestedConcept!!.variants
                    .distinctBy { Triple(it.commodityCode, it.officialName, it.market) }
                    .map { mapping ->
                        MoaWholesaleHistoryQuery(
                            from = backfillFrom,
                            to = backfillTo,
                            market = mapping.market,
                            cropName = mapping.officialName,
                        )
                    }

                catalogHistoryQueries.isNotEmpty() -> catalogHistoryQueries

                else -> emptyList()
            }
            if (snapshot == null && historyQueries.isEmpty()) {
                return SyncResult.Published(runId, 0, 0, 0)
            }
            val historySnapshots = historyQueries.mapIndexed { index, query ->
                onProgress(
                    PriceRefreshStage.HISTORY,
                    0.10f + 0.70f * index / historyQueries.size.coerceAtLeast(1),
                )
                client.fetchHistory(query).also {
                    onProgress(
                        PriceRefreshStage.HISTORY,
                        0.10f + 0.70f * (index + 1) / historyQueries.size.coerceAtLeast(1),
                    )
                }
            }
            val historyRecords = historySnapshots.flatMap { it.records }
            if (snapshot?.notModified == true && historyRecords.isEmpty()) {
                val run = SyncRunEntity(
                    runId = runId,
                    sourceKind = SourceKind.MOA_WHOLESALE,
                    startedAt = startedAt,
                    completedAt = snapshot.retrievedAt,
                    status = SourceDayState.VALID,
                    requestedFrom = null,
                    requestedTo = null,
                    pagesFetched = 0,
                    recordsAccepted = 0,
                    diagnostic = "Not modified; retained existing observations",
                )
                database.sourceDao().replaceRun(run)
                return SyncResult.Published(runId, 0, 0, 0)
            }
            val retrievedAt = listOfNotNull(snapshot?.retrievedAt) +
                historySnapshots.map { it.retrievedAt }
            val records = (snapshot?.records.orEmpty() + historyRecords)
                .asSequence()
                .filter { record ->
                    record.cropCode.equals("rest", ignoreCase = true) ||
                        record.cropCode in allowedCodes
                }
                .distinctBy { "${it.transactionDate}:${it.marketCode}:${it.cropCode}" }
                .toList()
            val effectiveRetrievedAt = retrievedAt.maxOrNull() ?: startedAt
            val validated = records.map { validateWholesale(it, effectiveRetrievedAt) }
            val invalid = validated.filterIsInstance<WholesaleValidation.Invalid>()
            if (invalid.isNotEmpty()) {
                database.sourceDao().replaceRun(
                    failedRun(
                        runId = runId,
                        startedAt = startedAt,
                        requestedFrom = requestedFrom,
                        requestedTo = requestedTo,
                        diagnostic = "${invalid.size} invalid records; publication skipped",
                    ),
                )
                return SyncResult.Failed(runId, "Invalid batch: ${invalid.size} records")
            }

            // The reviewed MOA endpoint is a complete daily snapshot rather than a paginated feed.
            // The stable identity and conditional headers still make repeat imports incremental at
            // the persistence boundary; pagination can be added if the upstream contract changes.
            val observations = validated
                .filterIsInstance<WholesaleValidation.Valid>()
                .map { it.observation.toEntity(runId) }
            val closures = validated.filterIsInstance<WholesaleValidation.Closed>()
            val closureKeys = closures.map { it.market to it.observedOn }.toSet()
            val observationKeys = observations.map { it.market to it.observedOn }.toSet()
            val conflictingClosureDays = closureKeys intersect observationKeys
            if (conflictingClosureDays.isNotEmpty()) {
                val diagnostic =
                    "${conflictingClosureDays.size} source days contain both closure and valid records"
                database.sourceDao().replaceRun(
                    failedRun(
                        runId = runId,
                        startedAt = startedAt,
                        requestedFrom = requestedFrom,
                        requestedTo = requestedTo,
                        diagnostic = diagnostic,
                    ),
                )
                return SyncResult.Failed(runId, diagnostic)
            }
            val validDayStates = observations
                .groupBy { it.market to it.observedOn }
                .map { (key, records) ->
                    val (market, date) = key
                    SourceDayStateEntity(
                        stateId = "${SourceKind.MOA_WHOLESALE.name}:${market.name}:$date",
                        sourceKind = SourceKind.MOA_WHOLESALE,
                        market = market,
                        observedOn = date,
                        state = classifySourceDay(
                            hasAnyRecords = records.isNotEmpty(),
                            hasInvalidRecords = false,
                            officialClosureConfirmed = false,
                        ),
                        diagnostic = null,
                        latestValidObservationOn = date,
                        updatedAt = effectiveRetrievedAt,
                        syncRunId = runId,
                    )
                }
            val closureDayStates = closureKeys.map { (market, date) ->
                val latestInSnapshot = observations
                    .asSequence()
                    .filter { it.market == market && it.observedOn < date }
                    .maxOfOrNull { it.observedOn }
                val latestStored = database.sourceDao().latestValidObservationDateBefore(
                    sourceKind = SourceKind.MOA_WHOLESALE,
                    market = market,
                    before = date,
                )
                SourceDayStateEntity(
                    stateId = "${SourceKind.MOA_WHOLESALE.name}:${market.name}:$date",
                    sourceKind = SourceKind.MOA_WHOLESALE,
                    market = market,
                    observedOn = date,
                    state = SourceDayState.CLOSED,
                    diagnostic = "Official closure signal (rest/休市)",
                    latestValidObservationOn = listOfNotNull(latestInSnapshot, latestStored).maxOrNull(),
                    updatedAt = effectiveRetrievedAt,
                    syncRunId = runId,
                )
            }
            val dayStates = validDayStates + closureDayStates
            onProgress(PriceRefreshStage.SAVING, 0.90f)
            val run = SyncRunEntity(
                runId = runId,
                sourceKind = SourceKind.MOA_WHOLESALE,
                startedAt = startedAt,
                completedAt = effectiveRetrievedAt,
                status = SourceDayState.VALID,
                requestedFrom = backfillFrom.takeIf { historyQueries.isNotEmpty() },
                requestedTo = backfillTo.takeIf { historyQueries.isNotEmpty() },
                pagesFetched = (if (snapshot == null || snapshot.notModified) 0 else 1) +
                    historySnapshots.sumOf { it.pagesFetched },
                recordsAccepted = observations.size,
                diagnostic = null,
            )
            database.sourceDao().publishImport(run, observations, dayStates)
            onProgress(PriceRefreshStage.SAVING, 0.98f)
            SyncResult.Published(runId, observations.size, 0, dayStates.size)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            database.sourceDao().replaceRun(
                failedRun(
                    runId = runId,
                    startedAt = startedAt,
                    requestedFrom = requestedFrom,
                    requestedTo = requestedTo,
                    diagnostic = failure.message ?: failure::class.simpleName.orEmpty(),
                ),
            )
            SyncResult.Failed(runId, failure.message ?: "Source synchronization failed")
        }
    }

    private fun failedRun(
        runId: String,
        startedAt: Instant,
        requestedFrom: LocalDate?,
        requestedTo: LocalDate?,
        diagnostic: String,
    ) = SyncRunEntity(
        runId = runId,
        sourceKind = SourceKind.MOA_WHOLESALE,
        startedAt = startedAt,
        completedAt = Instant.now(clock),
        status = SourceDayState.FAILED,
        requestedFrom = requestedFrom,
        requestedTo = requestedTo,
        pagesFetched = 0,
        recordsAccepted = 0,
        diagnostic = diagnostic,
    )

    private companion object {
        const val HISTORY_DAYS = 365
        const val CATALOG_HISTORY_DAYS = 30L
        const val BACKFILL_BOUNDARY_TOLERANCE_DAYS = 14L
    }
}

internal data class SyncDateRange(
    val from: LocalDate,
    val to: LocalDate,
)

internal fun missingDateRanges(
    from: LocalDate,
    to: LocalDate,
    completedDates: Collection<LocalDate>,
): List<SyncDateRange> {
    require(from <= to) { "History start must not be after end" }
    val completed = completedDates.asSequence()
        .filter { !it.isBefore(from) && !it.isAfter(to) }
        .toHashSet()
    val ranges = mutableListOf<SyncDateRange>()
    var missingFrom: LocalDate? = null
    var date = from
    while (!date.isAfter(to)) {
        if (date !in completed) {
            if (missingFrom == null) missingFrom = date
        } else if (missingFrom != null) {
            ranges += SyncDateRange(missingFrom, date.minusDays(1))
            missingFrom = null
        }
        date = date.plusDays(1)
    }
    if (missingFrom != null) {
        ranges += SyncDateRange(missingFrom, to)
    }
    return ranges
}

private fun tw.taipei.veges.domain.SourceObservation.toEntity(runId: String): SourceObservationEntity {
    val identity = "${source.name}:${market.name}:${commodityCode.value}:$observedOn"
    return SourceObservationEntity(
        observationId = identity,
        sourceKind = source,
        market = market,
        commodityCode = commodityCode.value,
        officialName = officialName,
        observedOn = observedOn,
        lowerPrice = lowerPrice?.amount,
        averagePrice = averagePrice?.amount,
        upperPrice = upperPrice?.amount,
        priceUnit = averagePrice?.unit,
        volume = volume,
        volumeUnit = "kg",
        state = state,
        sourceUrl = provenance.sourceUrl,
        attribution = provenance.attribution,
        retrievedAt = provenance.retrievedAt,
        syncRunId = runId,
    )
}
