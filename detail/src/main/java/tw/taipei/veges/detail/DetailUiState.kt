package tw.taipei.veges.detail

import tw.taipei.veges.domain.Estimate
import tw.taipei.veges.domain.ItemPriceDirectionEvaluation
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.ProduceConcept
import tw.taipei.veges.domain.UnavailableReason
import tw.taipei.veges.domain.TrendPeriod
import tw.taipei.veges.domain.TrendPoint
import tw.taipei.veges.domain.VariantMarketPrice

data class DetailUiState(
    val concept: ProduceConcept? = null,
    val selectedBasis: MarketBasis = MarketBasis.TAIPEI_COMBINED,
    val estimate: Estimate? = null,
    val estimateHistory: List<Estimate> = emptyList(),
    val unavailableReason: UnavailableReason? = null,
    val selectedPeriod: TrendPeriod = TrendPeriod.THIRTY_DAYS,
    val trendPoints: List<TrendPoint> = emptyList(),
    val variantPrices: List<VariantMarketPrice> = emptyList(),
    val isTracked: Boolean = false,
    val methodologyExpanded: Boolean = false,
    val untrackConfirmationCount: Int? = null,
    val priceDirectionEvaluation: ItemPriceDirectionEvaluation? = null,
)
