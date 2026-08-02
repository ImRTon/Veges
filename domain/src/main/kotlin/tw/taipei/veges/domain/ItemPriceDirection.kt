package tw.taipei.veges.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class ItemPriceDirection {
    RISING,
    FALLING,
}

enum class ItemPriceDirectionStatus {
    SIGNAL,
    NO_CLEAR_SIGNAL,
    INSUFFICIENT_HISTORY,
    STALE_DATA,
}

enum class ItemPriceDirectionReasonKind {
    TYPHOON,
    HEAVY_RAIN,
    EXTREME_HEAT,
    VOLUME_CONTRACTION,
    VOLUME_EXPANSION,
    PRICE_MOMENTUM_UP,
    PRICE_MOMENTUM_DOWN,
    ABOVE_RECENT_NORMAL,
    BELOW_RECENT_NORMAL,
}

data class ItemPriceDirectionReason(
    val kind: ItemPriceDirectionReasonKind,
    val contribution: Int,
    val headline: String,
    val shortLabel: String,
)

data class ItemPriceDirectionOutlook(
    val direction: ItemPriceDirection,
    val strengthScore: Int,
    val projectedChangePercent: BigDecimal,
    val reasons: List<ItemPriceDirectionReason>,
    val horizonStartDays: Int = 7,
    val horizonEndDays: Int = 14,
)

data class ItemPriceDirectionEvaluation(
    val status: ItemPriceDirectionStatus,
    val validTradingDayCount: Int,
    val latestObservationDate: LocalDate?,
    val outlook: ItemPriceDirectionOutlook? = null,
)

/**
 * Explainable, short-horizon direction analysis for one produce concept and market basis.
 *
 * The result is a qualified wholesale price-and-volume signal, not a future retail quote.
 */
class ItemPriceDirectionPredictor {
    fun evaluate(
        history: List<MarketHistoryPoint>,
        shockSignals: List<MarketShockSignal>,
        today: LocalDate = LocalDate.now(TAIPEI_ZONE),
        now: Instant = Instant.now(),
    ): ItemPriceDirectionEvaluation {
        val validHistory = history
            .filter { it.averageNtdPerKg > BigDecimal.ZERO && it.volumeKg > BigDecimal.ZERO }
            .distinctBy(MarketHistoryPoint::observedOn)
            .sortedByDescending(MarketHistoryPoint::observedOn)
        val latestDate = validHistory.firstOrNull()?.observedOn

        if (validHistory.size < MINIMUM_TRADING_DAYS) {
            return ItemPriceDirectionEvaluation(
                status = ItemPriceDirectionStatus.INSUFFICIENT_HISTORY,
                validTradingDayCount = validHistory.size,
                latestObservationDate = latestDate,
            )
        }
        if (requireNotNull(latestDate).isBefore(today.minusDays(MAX_SOURCE_AGE_DAYS))) {
            return ItemPriceDirectionEvaluation(
                status = ItemPriceDirectionStatus.STALE_DATA,
                validTradingDayCount = validHistory.size,
                latestObservationDate = latestDate,
            )
        }

        val recent = validHistory.take(RECENT_DAYS)
        val baseline = validHistory.drop(RECENT_DAYS).take(BASELINE_DAYS)
        val recentPrice = recent.averageOf(MarketHistoryPoint::averageNtdPerKg)
        val baselinePrice = baseline.averageOf(MarketHistoryPoint::averageNtdPerKg)
        val recentVolume = recent.averageOf(MarketHistoryPoint::volumeKg)
        val baselineVolume = baseline.averageOf(MarketHistoryPoint::volumeKg)
        val longerPrice = validHistory
            .drop(RECENT_DAYS)
            .take(LONG_BASELINE_DAYS)
            .map(MarketHistoryPoint::averageNtdPerKg)
            .median()

        val priceChangePercent = percentageChange(recentPrice, baselinePrice)
        val volumeChangePercent = percentageChange(recentVolume, baselineVolume)
        val anomalyPercent = percentageChange(recentPrice, longerPrice)
        val activeShocks = shockSignals.filter { it.effectiveAt <= now && it.expiresAt > now }

        val rising = risingCandidate(
            priceMomentum = priceChangePercent.coerceAtLeast(BigDecimal.ZERO),
            volumeContraction = volumeChangePercent.negate().coerceAtLeast(BigDecimal.ZERO),
            aboveNormal = anomalyPercent.coerceAtLeast(BigDecimal.ZERO),
            shocks = activeShocks,
        )
        val falling = fallingCandidate(
            priceMomentum = priceChangePercent.negate().coerceAtLeast(BigDecimal.ZERO),
            volumeExpansion = volumeChangePercent.coerceAtLeast(BigDecimal.ZERO),
            belowNormal = anomalyPercent.negate().coerceAtLeast(BigDecimal.ZERO),
        )
        val outlook = selectQualifiedCandidate(rising, falling)

        return ItemPriceDirectionEvaluation(
            status = if (outlook == null) {
                ItemPriceDirectionStatus.NO_CLEAR_SIGNAL
            } else {
                ItemPriceDirectionStatus.SIGNAL
            },
            validTradingDayCount = validHistory.size,
            latestObservationDate = latestDate,
            outlook = outlook,
        )
    }

    private fun risingCandidate(
        priceMomentum: BigDecimal,
        volumeContraction: BigDecimal,
        aboveNormal: BigDecimal,
        shocks: List<MarketShockSignal>,
    ): ItemPriceDirectionOutlook {
        val reasons = buildList {
            weatherReason(shocks)?.let(::add)
            if (volumeContraction >= BigDecimal("15")) {
                add(
                    ItemPriceDirectionReason(
                        kind = ItemPriceDirectionReasonKind.VOLUME_CONTRACTION,
                        contribution = scaledScore(volumeContraction, maxPercent = 50, maxScore = 30),
                        headline = "近期到貨量縮，供應壓力增加",
                        shortLabel = "到貨量縮",
                    ),
                )
            }
            if (priceMomentum >= BigDecimal("5")) {
                add(
                    ItemPriceDirectionReason(
                        kind = ItemPriceDirectionReasonKind.PRICE_MOMENTUM_UP,
                        contribution = scaledScore(priceMomentum, maxPercent = 25, maxScore = 40),
                        headline = "近期均價明顯轉強",
                        shortLabel = "價格轉強",
                    ),
                )
            }
            if (aboveNormal >= BigDecimal("10")) {
                add(
                    ItemPriceDirectionReason(
                        kind = ItemPriceDirectionReasonKind.ABOVE_RECENT_NORMAL,
                        contribution = scaledScore(aboveNormal, maxPercent = 30, maxScore = 25),
                        headline = "價格高於近期常態",
                        shortLabel = "高於常態",
                    ),
                )
            }
        }.sortedWith(
            compareByDescending<ItemPriceDirectionReason> { it.kind.isWeather() }
                .thenByDescending(ItemPriceDirectionReason::contribution),
        )
        val weatherUplift = when (reasons.firstOrNull { it.kind.isWeather() }?.kind) {
            ItemPriceDirectionReasonKind.TYPHOON -> BigDecimal("12")
            ItemPriceDirectionReasonKind.HEAVY_RAIN -> BigDecimal("8")
            ItemPriceDirectionReasonKind.EXTREME_HEAT -> BigDecimal("4")
            else -> BigDecimal.ZERO
        }
        val projected = (
            priceMomentum * BigDecimal("0.65") +
                volumeContraction * BigDecimal("0.25") +
                aboveNormal * BigDecimal("0.15") +
                weatherUplift
            ).coerceAtMost(BigDecimal("60"))
            .setScale(1, RoundingMode.HALF_UP)
        return ItemPriceDirectionOutlook(
            direction = ItemPriceDirection.RISING,
            strengthScore = reasons.sumOf(ItemPriceDirectionReason::contribution).coerceAtMost(100),
            projectedChangePercent = projected,
            reasons = reasons,
        )
    }

    private fun fallingCandidate(
        priceMomentum: BigDecimal,
        volumeExpansion: BigDecimal,
        belowNormal: BigDecimal,
    ): ItemPriceDirectionOutlook {
        val reasons = buildList {
            if (volumeExpansion >= BigDecimal("15")) {
                add(
                    ItemPriceDirectionReason(
                        kind = ItemPriceDirectionReasonKind.VOLUME_EXPANSION,
                        contribution = scaledScore(volumeExpansion, maxPercent = 50, maxScore = 30),
                        headline = "近期到貨量增加，供應較寬鬆",
                        shortLabel = "到貨量增",
                    ),
                )
            }
            if (priceMomentum >= BigDecimal("5")) {
                add(
                    ItemPriceDirectionReason(
                        kind = ItemPriceDirectionReasonKind.PRICE_MOMENTUM_DOWN,
                        contribution = scaledScore(priceMomentum, maxPercent = 25, maxScore = 40),
                        headline = "近期均價明顯轉弱",
                        shortLabel = "價格轉弱",
                    ),
                )
            }
            if (belowNormal >= BigDecimal("10")) {
                add(
                    ItemPriceDirectionReason(
                        kind = ItemPriceDirectionReasonKind.BELOW_RECENT_NORMAL,
                        contribution = scaledScore(belowNormal, maxPercent = 30, maxScore = 25),
                        headline = "價格低於近期常態",
                        shortLabel = "低於常態",
                    ),
                )
            }
        }.sortedByDescending(ItemPriceDirectionReason::contribution)
        val projectedMagnitude = (
            priceMomentum * BigDecimal("0.70") +
                volumeExpansion * BigDecimal("0.20") +
                belowNormal * BigDecimal("0.15")
            ).coerceAtMost(BigDecimal("50"))
            .setScale(1, RoundingMode.HALF_UP)
        return ItemPriceDirectionOutlook(
            direction = ItemPriceDirection.FALLING,
            strengthScore = reasons.sumOf(ItemPriceDirectionReason::contribution).coerceAtMost(100),
            projectedChangePercent = projectedMagnitude.negate(),
            reasons = reasons,
        )
    }

    private fun selectQualifiedCandidate(
        rising: ItemPriceDirectionOutlook,
        falling: ItemPriceDirectionOutlook,
    ): ItemPriceDirectionOutlook? {
        val qualified = listOf(rising, falling).filter { candidate ->
            candidate.strengthScore >= MINIMUM_STRENGTH_SCORE &&
                candidate.projectedChangePercent.abs() >= MINIMUM_PROJECTED_CHANGE &&
                candidate.reasons.any { !it.kind.isWeather() }
        }.sortedByDescending(ItemPriceDirectionOutlook::strengthScore)
        if (qualified.size > 1 &&
            qualified[0].strengthScore - qualified[1].strengthScore < MINIMUM_DIRECTION_SCORE_LEAD
        ) {
            return null
        }
        return qualified.firstOrNull()
    }

    private fun weatherReason(signals: List<MarketShockSignal>): ItemPriceDirectionReason? =
        signals
            .filter { signal ->
                signal.affectedAreas.isEmpty() ||
                    signal.affectedAreas.any { area ->
                        AGRICULTURAL_AREAS.any(area::startsWith)
                    }
            }
            .map { signal ->
                when (signal.kind) {
                    MarketShockKind.TYPHOON -> ItemPriceDirectionReason(
                        kind = ItemPriceDirectionReasonKind.TYPHOON,
                        contribution = (25 * signal.severity.toDouble()).toInt().coerceIn(15, 25),
                        headline = "颱風警報可能加重供應壓力",
                        shortLabel = "颱風警報",
                    )

                    MarketShockKind.HEAVY_RAIN -> ItemPriceDirectionReason(
                        kind = ItemPriceDirectionReasonKind.HEAVY_RAIN,
                        contribution = (20 * signal.severity.toDouble()).toInt().coerceIn(12, 20),
                        headline = "豪雨可能影響產地供應",
                        shortLabel = "豪雨影響",
                    )

                    MarketShockKind.EXTREME_HEAT -> ItemPriceDirectionReason(
                        kind = ItemPriceDirectionReasonKind.EXTREME_HEAT,
                        contribution = (10 * signal.severity.toDouble()).toInt().coerceIn(5, 10),
                        headline = "高溫可能增加供應壓力",
                        shortLabel = "高溫影響",
                    )
                }
            }
            .maxByOrNull(ItemPriceDirectionReason::contribution)

    private fun percentageChange(current: BigDecimal, reference: BigDecimal): BigDecimal {
        if (reference <= BigDecimal.ZERO) return BigDecimal.ZERO
        return current.subtract(reference)
            .divide(reference, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal("100"))
    }

    private fun scaledScore(value: BigDecimal, maxPercent: Int, maxScore: Int): Int =
        value.divide(BigDecimal(maxPercent), 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal(maxScore))
            .toInt()
            .coerceIn(0, maxScore)

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

    private fun ItemPriceDirectionReasonKind.isWeather(): Boolean =
        this == ItemPriceDirectionReasonKind.TYPHOON ||
            this == ItemPriceDirectionReasonKind.HEAVY_RAIN ||
            this == ItemPriceDirectionReasonKind.EXTREME_HEAT

    private companion object {
        val TAIPEI_ZONE: ZoneId = ZoneId.of("Asia/Taipei")
        val MINIMUM_PROJECTED_CHANGE = BigDecimal("8")
        const val MINIMUM_TRADING_DAYS = 10
        const val MAX_SOURCE_AGE_DAYS = 4L
        const val RECENT_DAYS = 3
        const val BASELINE_DAYS = 7
        const val LONG_BASELINE_DAYS = 20
        const val MINIMUM_STRENGTH_SCORE = 40
        const val MINIMUM_DIRECTION_SCORE_LEAD = 10
        val AGRICULTURAL_AREAS = setOf(
            "桃園市",
            "宜蘭縣",
            "苗栗縣",
            "臺中市",
            "彰化縣",
            "南投縣",
            "雲林縣",
            "嘉義縣",
            "嘉義市",
            "臺南市",
            "高雄市",
            "屏東縣",
            "花蓮縣",
            "臺東縣",
        )
    }
}
