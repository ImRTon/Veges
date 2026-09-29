package tw.taipei.veges.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

enum class MarketShockKind {
    TYPHOON,
    HEAVY_RAIN,
    EXTREME_HEAT,
}

data class MarketShockSignal(
    val kind: MarketShockKind,
    val severity: BigDecimal,
    val headline: String,
    val affectedAreas: Set<String>,
    val effectiveAt: Instant,
    val expiresAt: Instant,
    val cause: MarketShockKind? = null,
)

interface MarketShockRepository {
    val signals: Flow<List<MarketShockSignal>>

    suspend fun refresh()
}

enum class PriceSurgeReasonKind {
    TYPHOON,
    HEAVY_RAIN,
    EXTREME_HEAT,
    VOLUME_CONTRACTION,
    PRICE_MOMENTUM,
    RECENT_PRICE_ANOMALY,
}

data class PriceSurgeReason(
    val kind: PriceSurgeReasonKind,
    val contribution: Int,
    val headline: String,
    val shortLabel: String,
)

enum class PriceSurgeRiskLevel {
    ELEVATED,
    HIGH,
}

data class PriceSurgePrediction(
    val concept: ProduceConcept,
    val riskScore: Int,
    val riskLevel: PriceSurgeRiskLevel,
    val projectedRisePercent: BigDecimal,
    val horizonStartDays: Int = 7,
    val horizonEndDays: Int = 14,
    val reasons: List<PriceSurgeReason>,
)

data class MarketPriceSurgeOutlook(
    val riskScore: Int,
    val riskLevel: PriceSurgeRiskLevel,
    val projectedRisePercent: BigDecimal,
    val affectedItemCount: Int,
    val eligibleItemCount: Int,
    val marketBreadthPercent: Int,
    val primaryReason: PriceSurgeReason,
    val affectedNames: List<String>,
    val horizonStartDays: Int = 7,
    val horizonEndDays: Int = 14,
)

data class PriceSurgeEvaluation(
    val predictions: List<PriceSurgePrediction>,
    val eligibleItemCount: Int,
    val marketOutlook: MarketPriceSurgeOutlook?,
)

/**
 * A deliberately explainable short-horizon warning model.
 *
 * It estimates price-surge risk rather than a future retail price. Every alert must be supported
 * by at least ten valid trading days and observed wholesale market evidence.
 */
class PriceSurgePredictor {
    fun evaluate(
        items: List<MarketItem>,
        @Suppress("UNUSED_PARAMETER")
        shockSignals: List<MarketShockSignal>,
        today: LocalDate = LocalDate.now(TAIPEI_ZONE),
        @Suppress("UNUSED_PARAMETER")
        now: Instant = Instant.now(),
    ): PriceSurgeEvaluation {
        var eligibleCount = 0
        val predictions = items.mapNotNull { item ->
            val history = item.wholesaleHistory
                .filter { it.averageNtdPerKg > BigDecimal.ZERO && it.volumeKg > BigDecimal.ZERO }
                .distinctBy(MarketHistoryPoint::observedOn)
                .sortedByDescending(MarketHistoryPoint::observedOn)
            if (history.size < MINIMUM_TRADING_DAYS ||
                history.first().observedOn.isBefore(today.minusDays(MAX_SOURCE_AGE_DAYS))
            ) {
                return@mapNotNull null
            }
            eligibleCount += 1
            predict(item, history)
        }.sortedWith(
            compareByDescending<PriceSurgePrediction>(PriceSurgePrediction::riskScore)
                .thenByDescending(PriceSurgePrediction::projectedRisePercent),
        )

        return PriceSurgeEvaluation(
            predictions = predictions,
            eligibleItemCount = eligibleCount,
            marketOutlook = buildMarketOutlook(predictions, eligibleCount),
        )
    }

    private fun buildMarketOutlook(
        predictions: List<PriceSurgePrediction>,
        eligibleItemCount: Int,
    ): MarketPriceSurgeOutlook? {
        if (eligibleItemCount < MINIMUM_MARKET_SAMPLE_SIZE ||
            predictions.size < MINIMUM_AFFECTED_ITEMS
        ) {
            return null
        }
        val breadthPercent = predictions.size * 100 / eligibleItemCount
        if (breadthPercent < MINIMUM_MARKET_BREADTH_PERCENT) return null

        val dominantReason = predictions
            .flatMap(PriceSurgePrediction::reasons)
            .groupBy(PriceSurgeReason::kind)
            .maxWithOrNull(
                compareBy<Map.Entry<PriceSurgeReasonKind, List<PriceSurgeReason>>> {
                    it.value.size
                }.thenBy {
                    it.value.sumOf(PriceSurgeReason::contribution)
                }.thenBy {
                    it.key.marketPriority()
                },
            )
            ?.value
            ?.maxByOrNull(PriceSurgeReason::contribution)
            ?: return null
        val marketEvidenceReason = dominantReason.copy(
            headline = dominantReason.kind.marketHeadline(),
        )
        val primaryReason = marketEvidenceReason
        val averageRiskScore = predictions
            .map(PriceSurgePrediction::riskScore)
            .average()
            .toInt()
        val averageRise = predictions
            .map(PriceSurgePrediction::projectedRisePercent)
            .reduce(BigDecimal::add)
            .divide(BigDecimal(predictions.size), 1, RoundingMode.HALF_UP)

        return MarketPriceSurgeOutlook(
            riskScore = averageRiskScore,
            riskLevel = if (averageRiskScore >= HIGH_RISK_SCORE) {
                PriceSurgeRiskLevel.HIGH
            } else {
                PriceSurgeRiskLevel.ELEVATED
            },
            projectedRisePercent = averageRise,
            affectedItemCount = predictions.size,
            eligibleItemCount = eligibleItemCount,
            marketBreadthPercent = breadthPercent,
            primaryReason = primaryReason,
            affectedNames = predictions
                .map { it.concept.householdName }
                .distinct()
                .take(MAX_AFFECTED_NAMES),
        )
    }

    private fun predict(
        item: MarketItem,
        history: List<MarketHistoryPoint>,
    ): PriceSurgePrediction? {
        val recent = history.take(RECENT_DAYS)
        val baseline = history.drop(RECENT_DAYS).take(BASELINE_DAYS)
        if (recent.size < RECENT_DAYS || baseline.size < BASELINE_DAYS) return null

        val recentPrice = recent.averageOf(MarketHistoryPoint::averageNtdPerKg)
        val baselinePrice = baseline.averageOf(MarketHistoryPoint::averageNtdPerKg)
        val recentVolume = recent.averageOf(MarketHistoryPoint::volumeKg)
        val baselineVolume = baseline.averageOf(MarketHistoryPoint::volumeKg)
        val longerPrice = history.drop(RECENT_DAYS)
            .take(LONG_BASELINE_DAYS)
            .map(MarketHistoryPoint::averageNtdPerKg)
            .median()

        val momentumPercent = positivePercentageChange(recentPrice, baselinePrice)
        val volumeDropPercent = positivePercentageChange(baselineVolume, recentVolume)
        val anomalyPercent = positivePercentageChange(recentPrice, longerPrice)
        val volumeQualifies = volumeDropPercent >= VOLUME_CONTRACTION_THRESHOLD
        val momentumQualifies = momentumPercent >= PRICE_MOMENTUM_THRESHOLD
        val anomalyQualifies = anomalyPercent >= RECENT_ANOMALY_THRESHOLD

        val reasons = buildList {
            if (volumeQualifies) {
                add(
                    PriceSurgeReason(
                        kind = PriceSurgeReasonKind.VOLUME_CONTRACTION,
                        contribution = scaledScore(volumeDropPercent, maxPercent = 50, maxScore = 30),
                        headline = "到貨量縮，即將漲價",
                        shortLabel = "到貨量縮",
                    ),
                )
            }
            if (momentumQualifies) {
                add(
                    PriceSurgeReason(
                        kind = PriceSurgeReasonKind.PRICE_MOMENTUM,
                        contribution = scaledScore(momentumPercent, maxPercent = 20, maxScore = 30),
                        headline = "價格加速上揚",
                        shortLabel = "價格轉強",
                    ),
                )
            }
            if (anomalyQualifies) {
                add(
                    PriceSurgeReason(
                        kind = PriceSurgeReasonKind.RECENT_PRICE_ANOMALY,
                        contribution = scaledScore(anomalyPercent, maxPercent = 30, maxScore = 15),
                        headline = "價格高於近期常態",
                        shortLabel = "偏離常態",
                    ),
                )
            }
        }.sortedByDescending(PriceSurgeReason::contribution)

        val projectedRise = (
            momentumPercent.takeIf { momentumQualifies }.orZero() * BigDecimal("0.65") +
                volumeDropPercent.takeIf { volumeQualifies }.orZero() * BigDecimal("0.25") +
                anomalyPercent.takeIf { anomalyQualifies }.orZero() * BigDecimal("0.15")
            ).coerceAtMost(BigDecimal("60"))
            .setScale(1, RoundingMode.HALF_UP)
        val riskScore = reasons.sumOf(PriceSurgeReason::contribution).coerceAtMost(100)

        if (projectedRise < MINIMUM_PROJECTED_RISE || riskScore < MINIMUM_RISK_SCORE) return null

        return PriceSurgePrediction(
            concept = item.concept,
            riskScore = riskScore,
            riskLevel = if (riskScore >= HIGH_RISK_SCORE) {
                PriceSurgeRiskLevel.HIGH
            } else {
                PriceSurgeRiskLevel.ELEVATED
            },
            projectedRisePercent = projectedRise,
            reasons = reasons,
        )
    }


    private fun scaledScore(value: BigDecimal, maxPercent: Int, maxScore: Int): Int =
        value.divide(BigDecimal(maxPercent), 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal(maxScore))
            .toInt()
            .coerceIn(0, maxScore)

    private fun positivePercentageChange(current: BigDecimal, reference: BigDecimal): BigDecimal {
        if (reference <= BigDecimal.ZERO) return BigDecimal.ZERO
        return current.subtract(reference)
            .divide(reference, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal("100"))
            .coerceAtLeast(BigDecimal.ZERO)
    }

    private fun List<BigDecimal>.median(): BigDecimal {
        if (isEmpty()) return BigDecimal.ZERO
        val ordered = sorted()
        return if (ordered.size % 2 == 1) {
            ordered[ordered.size / 2]
        } else {
            ordered[ordered.size / 2 - 1]
                .add(ordered[ordered.size / 2])
                .divide(BigDecimal("2"), 6, RoundingMode.HALF_UP)
        }
    }

    private fun List<MarketHistoryPoint>.averageOf(
        selector: (MarketHistoryPoint) -> BigDecimal,
    ): BigDecimal = map(selector)
        .reduce(BigDecimal::add)
        .divide(BigDecimal(size), 6, RoundingMode.HALF_UP)

    private fun BigDecimal?.orZero(): BigDecimal = this ?: BigDecimal.ZERO

    private fun PriceSurgeReasonKind.marketPriority(): Int = when (this) {
        PriceSurgeReasonKind.TYPHOON -> 6
        PriceSurgeReasonKind.HEAVY_RAIN -> 5
        PriceSurgeReasonKind.EXTREME_HEAT -> 4
        PriceSurgeReasonKind.VOLUME_CONTRACTION -> 3
        PriceSurgeReasonKind.PRICE_MOMENTUM -> 2
        PriceSurgeReasonKind.RECENT_PRICE_ANOMALY -> 1
    }

    private fun PriceSurgeReasonKind.marketHeadline(): String = when (this) {
        PriceSurgeReasonKind.TYPHOON -> "颱風來襲，整體蔬果價格可能上揚"
        PriceSurgeReasonKind.HEAVY_RAIN -> "連續暴雨，整體蔬果價格可能上揚"
        PriceSurgeReasonKind.EXTREME_HEAT -> "高溫持續，整體蔬果供應承壓"
        PriceSurgeReasonKind.VOLUME_CONTRACTION -> "到貨量普遍縮減，整體價格可能上揚"
        PriceSurgeReasonKind.PRICE_MOMENTUM -> "多項蔬果價格同步上揚"
        PriceSurgeReasonKind.RECENT_PRICE_ANOMALY -> "多項蔬果價格高於近期常態"
    }

    private companion object {
        val TAIPEI_ZONE: ZoneId = ZoneId.of("Asia/Taipei")
        val MINIMUM_PROJECTED_RISE = BigDecimal("20")
        val VOLUME_CONTRACTION_THRESHOLD = BigDecimal("15")
        val PRICE_MOMENTUM_THRESHOLD = BigDecimal("5")
        val RECENT_ANOMALY_THRESHOLD = BigDecimal("10")
        const val MINIMUM_TRADING_DAYS = 10
        const val MAX_SOURCE_AGE_DAYS = 4L
        const val RECENT_DAYS = 3
        const val BASELINE_DAYS = 7
        const val LONG_BASELINE_DAYS = 20
        const val MINIMUM_RISK_SCORE = 60
        const val HIGH_RISK_SCORE = 75
        const val MINIMUM_MARKET_SAMPLE_SIZE = 10
        const val MINIMUM_AFFECTED_ITEMS = 3
        const val MINIMUM_MARKET_BREADTH_PERCENT = 20
        const val MAX_AFFECTED_NAMES = 4
    }
}
