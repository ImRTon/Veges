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
    suspend fun synchronizeConceptHistory(conceptId: String): SyncResult =
        synchronize(requestedConceptId = conceptId)

    suspend fun synchronize(
        requestedFrom: LocalDate? = null,
        requestedTo: LocalDate? = null,
        requestedConceptId: String? = null,
        etag: String? = null,
        lastModified: String? = null,
    ): SyncResult {
        val runId = UUID.randomUUID().toString()
        val startedAt = Instant.now(clock)
        return try {
            val snapshot = client.fetch(etag, lastModified)
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
            val bootstrapFrom = today.minusDays(CATALOG_BOOTSTRAP_DAYS - 1L)
            val expectedVegetableCodes = database.taxonomyDao().publishedVegetableCommodityCodeCount()
            val recentVegetableCodes = database.sourceDao()
                .recentObservedPublishedVegetableCodeCount(bootstrapFrom)
            val recentVegetableTradingDays = database.sourceDao()
                .recentPublishedVegetableTradingDayCount(bootstrapFrom)
            val needsCatalogBootstrap = requestedConcept == null &&
                needsCatalogBootstrap(
                    expectedVegetableCodes = expectedVegetableCodes,
                    recentVegetableCodes = recentVegetableCodes,
                    recentTradingDays = recentVegetableTradingDays,
                )
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

                needsCatalogBootstrap -> listOf(
                    MarketBasis.TAIPEI_FIRST,
                    MarketBasis.TAIPEI_SECOND,
                ).map { market ->
                    MoaWholesaleHistoryQuery(
                        from = bootstrapFrom,
                        to = backfillTo,
                        market = market,
                    )
                }

                else -> emptyList()
            }
            val historySnapshots = historyQueries.map { client.fetchHistory(it) }
            val historyRecords = historySnapshots.flatMap { it.records }
            if (snapshot.notModified && historyRecords.isEmpty()) {
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
            val retrievedAt = listOf(snapshot.retrievedAt) +
                historySnapshots.map { it.retrievedAt }
            val records = (snapshot.records + historyRecords)
                .asSequence()
                .filter { record ->
                    record.cropCode.equals("rest", ignoreCase = true) ||
                        record.cropCode in allowedCodes
                }
                .distinctBy { "${it.transactionDate}:${it.marketCode}:${it.cropCode}" }
                .toList()
            val effectiveRetrievedAt = retrievedAt.maxOrNull() ?: snapshot.retrievedAt
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
            val run = SyncRunEntity(
                runId = runId,
                sourceKind = SourceKind.MOA_WHOLESALE,
                startedAt = startedAt,
                completedAt = effectiveRetrievedAt,
                status = SourceDayState.VALID,
                requestedFrom = backfillFrom.takeIf { historyQueries.isNotEmpty() },
                requestedTo = backfillTo.takeIf { historyQueries.isNotEmpty() },
                pagesFetched = (if (snapshot.notModified) 0 else 1) +
                    historySnapshots.sumOf { it.pagesFetched },
                recordsAccepted = observations.size,
                diagnostic = null,
            )
            database.sourceDao().publishImport(run, observations, dayStates)
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
        const val CATALOG_BOOTSTRAP_DAYS = 60L
        const val MIN_CATALOG_TRADING_DAYS = 31
        const val BACKFILL_BOUNDARY_TOLERANCE_DAYS = 14L
        const val MIN_CATALOG_COVERAGE_RATIO = 0.8
    }
}

internal fun needsCatalogBootstrap(
    expectedVegetableCodes: Int,
    recentVegetableCodes: Int,
    recentTradingDays: Int,
): Boolean {
    if (expectedVegetableCodes <= 0) return false
    val coverage = recentVegetableCodes.toDouble() / expectedVegetableCodes
    return coverage < 0.8 || recentTradingDays < 31
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
