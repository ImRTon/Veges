package tw.taipei.veges.data.sync

import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import tw.taipei.veges.data.estimation.TemporaryFactorArtifactLoader
import tw.taipei.veges.data.local.EstimateEntity
import tw.taipei.veges.data.local.SourceObservationEntity
import tw.taipei.veges.data.local.TaxonomyConceptWithDetails
import tw.taipei.veges.data.local.VegesDatabase
import tw.taipei.veges.data.repository.toEntity
import tw.taipei.veges.domain.EstimationMath
import tw.taipei.veges.domain.EstimatorFeatureSet
import tw.taipei.veges.domain.EstimatorRegistry
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.PriceUnit
import tw.taipei.veges.domain.ProduceConceptId
import tw.taipei.veges.domain.RuntimeEstimateResult

fun interface EstimateRefreshCoordinator {
    suspend fun calculateNewEstimates(): List<EstimateEntity>
}

class RoomTemporaryFactorEstimateRefreshCoordinator @Inject constructor(
    private val database: VegesDatabase,
    private val artifactLoader: TemporaryFactorArtifactLoader,
    private val clock: Clock,
) : EstimateRefreshCoordinator {
    override suspend fun calculateNewEstimates(): List<EstimateEntity> {
        val artifact = artifactLoader.load()
        val registry = EstimatorRegistry(clock = clock)
        val estimates = database.taxonomyDao()
            .publishedConceptsWithDetails()
            .flatMap { concept ->
                val byMarket = loadObservations(concept)
                MarketBasis.entries.flatMap { basis ->
                    aggregationsForBasis(basis, byMarket).mapNotNull { input ->
                        val result = registry.estimate(
                            artifact = artifact,
                            conceptId = ProduceConceptId(concept.concept.stableId),
                            basis = basis,
                            sourceDate = input.sourceDate,
                            sourceDates = input.rows.map { it.observedOn }.distinct().sorted(),
                            features = EstimatorFeatureSet(
                                wholesaleAverageNtdPerKg = input.averageNtdPerKg,
                                wholesaleLowerNtdPerKg = input.lowerNtdPerKg,
                                wholesaleUpperNtdPerKg = input.upperNtdPerKg,
                                transactionVolumeKg = input.volumeKg,
                                monthOfYear = input.sourceDate.monthValue,
                                marketBasis = basis,
                            ),
                            sourceRetrievedAt = input.rows.minOf { it.retrievedAt },
                        )
                        val estimate = (result as? RuntimeEstimateResult.Available)?.estimate
                            ?: return@mapNotNull null
                        estimate.toEntity(
                            estimateId = stableEstimateId(
                                concept.concept.stableId,
                                basis,
                                input.sourceDate,
                                artifact.modelVersion,
                            ),
                            pairedCalibrationPeriods = 0,
                        )
                    }
                }
            }
        if (estimates.isNotEmpty()) {
            database.estimateDao().replaceEstimates(estimates)
        }
        return estimates
    }

    private suspend fun loadObservations(
        concept: TaxonomyConceptWithDetails,
    ): Map<MarketBasis, List<SourceObservationEntity>> =
        listOf(MarketBasis.TAIPEI_FIRST, MarketBasis.TAIPEI_SECOND).associateWith { market ->
            val codes = concept.variants
                .asSequence()
                .filter { it.market == market }
                .map { it.commodityCode }
                .distinct()
                .toList()
            if (codes.isEmpty()) {
                emptyList()
            } else {
                database.sourceDao().validWholesaleObservations(codes, market)
            }
        }

    private fun aggregationsForBasis(
        basis: MarketBasis,
        byMarket: Map<MarketBasis, List<SourceObservationEntity>>,
    ): List<AggregatedWholesale> = when (basis) {
        MarketBasis.TAIPEI_FIRST,
        MarketBasis.TAIPEI_SECOND,
        -> validAggregations(byMarket.getValue(basis))

        MarketBasis.TAIPEI_COMBINED -> {
            val firstByDate = byMarket.getValue(MarketBasis.TAIPEI_FIRST).groupBy { it.observedOn }
            val secondByDate = byMarket.getValue(MarketBasis.TAIPEI_SECOND).groupBy { it.observedOn }
            (firstByDate.keys intersect secondByDate.keys)
                .sortedDescending()
                .mapNotNull { date ->
                    aggregate(firstByDate.getValue(date) + secondByDate.getValue(date))
                }
                .take(MAX_ESTIMATE_TRADING_DAYS)
        }
    }

    private fun validAggregations(rows: List<SourceObservationEntity>): List<AggregatedWholesale> =
        rows.groupBy { it.observedOn }
            .toSortedMap(reverseOrder())
            .values
            .mapNotNull(::aggregate)
            .take(MAX_ESTIMATE_TRADING_DAYS)

    private fun aggregate(rows: List<SourceObservationEntity>): AggregatedWholesale? {
        if (rows.isEmpty() ||
            rows.any {
                it.priceUnit != PriceUnit.NTD_PER_KILOGRAM ||
                    it.averagePrice == null ||
                    it.lowerPrice == null ||
                    it.upperPrice == null ||
                    it.volume == null ||
                    it.averagePrice <= BigDecimal.ZERO ||
                    it.lowerPrice <= BigDecimal.ZERO ||
                    it.upperPrice <= BigDecimal.ZERO ||
                    it.volume <= BigDecimal.ZERO
            }
        ) {
            return null
        }
        val weightedAverage = EstimationMath.weightedAverage(
            rows.map { requireNotNull(it.averagePrice) to requireNotNull(it.volume) },
        ) ?: return null
        val weightedLower = EstimationMath.weightedAverage(
            rows.map { requireNotNull(it.lowerPrice) to requireNotNull(it.volume) },
        ) ?: return null
        val weightedUpper = EstimationMath.weightedAverage(
            rows.map { requireNotNull(it.upperPrice) to requireNotNull(it.volume) },
        ) ?: return null
        return AggregatedWholesale(
            sourceDate = rows.first().observedOn,
            averageNtdPerKg = weightedAverage,
            lowerNtdPerKg = weightedLower,
            upperNtdPerKg = weightedUpper,
            volumeKg = rows.sumOf { requireNotNull(it.volume) },
            rows = rows,
        )
    }

    private fun stableEstimateId(
        conceptId: String,
        basis: MarketBasis,
        sourceDate: LocalDate,
        modelVersion: String,
    ): String {
        val input = "$conceptId|${basis.name}|$sourceDate|$modelVersion"
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val MAX_ESTIMATE_TRADING_DAYS = 31
    }
}

private data class AggregatedWholesale(
    val sourceDate: LocalDate,
    val averageNtdPerKg: BigDecimal,
    val lowerNtdPerKg: BigDecimal,
    val upperNtdPerKg: BigDecimal,
    val volumeKg: BigDecimal,
    val rows: List<SourceObservationEntity>,
)
