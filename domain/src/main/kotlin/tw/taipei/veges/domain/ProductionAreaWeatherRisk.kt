package tw.taipei.veges.domain

import java.math.BigDecimal
import java.time.Instant

data class ProductionAreaWeatherRisk(
    val kind: MarketShockKind,
    val affectedCounties: List<String>,
    val severity: BigDecimal,
    val expiresAt: Instant,
)

class ProductionAreaWeatherRiskEvaluator {
    fun evaluate(
        signals: List<MarketShockSignal>,
        now: Instant = Instant.now(),
    ): ProductionAreaWeatherRisk? = signals
        .asSequence()
        .filter { signal -> signal.effectiveAt <= now && signal.expiresAt > now }
        .mapNotNull { signal ->
            val counties = signal.affectedAgriculturalCounties()
            if (counties.isEmpty()) return@mapNotNull null
            QualifiedProductionAreaSignal(
                kind = signal.effectiveWeatherKind(),
                counties = counties,
                severity = signal.severity,
                expiresAt = signal.expiresAt,
            )
        }
        .groupBy(QualifiedProductionAreaSignal::kind)
        .map { (kind, matches) ->
            ProductionAreaWeatherRisk(
                kind = kind,
                affectedCounties = AGRICULTURAL_COUNTIES.filter { county ->
                    matches.any { county in it.counties }
                },
                severity = matches.maxOf(QualifiedProductionAreaSignal::severity),
                expiresAt = matches.maxOf(QualifiedProductionAreaSignal::expiresAt),
            )
        }
        .maxWithOrNull(
            compareBy<ProductionAreaWeatherRisk> { it.kind.productionRiskPriority() }
                .thenBy(ProductionAreaWeatherRisk::severity)
                .thenBy { it.affectedCounties.size },
        )
}

private data class QualifiedProductionAreaSignal(
    val kind: MarketShockKind,
    val counties: Set<String>,
    val severity: BigDecimal,
    val expiresAt: Instant,
)

private fun agriculturalCountyFor(area: String): String? =
    AGRICULTURAL_COUNTIES.firstOrNull(area::startsWith)

internal fun MarketShockSignal.affectedAgriculturalCounties(): Set<String> =
    affectedAreas.mapNotNull(::agriculturalCountyFor).toSet()

internal fun MarketShockSignal.effectiveWeatherKind(): MarketShockKind = cause ?: kind

private fun MarketShockKind.productionRiskPriority(): Int = when (this) {
    MarketShockKind.TYPHOON -> 3
    MarketShockKind.HEAVY_RAIN -> 2
    MarketShockKind.EXTREME_HEAT -> 1
}

internal val AGRICULTURAL_COUNTIES = listOf(
    "桃園市",
    "新竹縣",
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
