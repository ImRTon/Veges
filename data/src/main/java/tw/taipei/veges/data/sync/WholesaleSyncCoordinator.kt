package tw.taipei.veges.data.sync

import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import tw.taipei.veges.data.local.SourceDayStateEntity
import tw.taipei.veges.data.local.SourceObservationEntity
import tw.taipei.veges.data.local.SyncRunEntity
import tw.taipei.veges.data.local.VegesDatabase
import tw.taipei.veges.data.network.MoaWholesaleClient
import tw.taipei.veges.data.network.WholesaleValidation
import tw.taipei.veges.data.network.classifySourceDay
import tw.taipei.veges.data.network.validateWholesale
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
) {
    suspend fun synchronize(
        requestedFrom: LocalDate? = null,
        requestedTo: LocalDate? = null,
        etag: String? = null,
        lastModified: String? = null,
    ): SyncResult {
        val runId = UUID.randomUUID().toString()
        val startedAt = Instant.now()
        return try {
            val snapshot = client.fetch(etag, lastModified)
            if (snapshot.notModified) {
                val run = SyncRunEntity(
                    runId = runId,
                    sourceKind = SourceKind.MOA_WHOLESALE,
                    startedAt = startedAt,
                    completedAt = snapshot.retrievedAt,
                    status = SourceDayState.VALID,
                    requestedFrom = requestedFrom,
                    requestedTo = requestedTo,
                    pagesFetched = 0,
                    recordsAccepted = 0,
                    diagnostic = "Not modified; retained existing observations",
                )
                database.sourceDao().replaceRun(run)
                return SyncResult.Published(runId, 0, 0, 0)
            }
            val validated = snapshot.records.map { validateWholesale(it, snapshot.retrievedAt) }
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
            val dayStates = observations
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
                        updatedAt = snapshot.retrievedAt,
                        syncRunId = runId,
                    )
                }
            val run = SyncRunEntity(
                runId = runId,
                sourceKind = SourceKind.MOA_WHOLESALE,
                startedAt = startedAt,
                completedAt = snapshot.retrievedAt,
                status = SourceDayState.VALID,
                requestedFrom = requestedFrom,
                requestedTo = requestedTo,
                pagesFetched = 1,
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
        completedAt = Instant.now(),
        status = SourceDayState.FAILED,
        requestedFrom = requestedFrom,
        requestedTo = requestedTo,
        pagesFetched = 0,
        recordsAccepted = 0,
        diagnostic = diagnostic,
    )
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
