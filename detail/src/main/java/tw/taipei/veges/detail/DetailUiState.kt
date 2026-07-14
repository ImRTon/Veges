package tw.taipei.veges.detail

import tw.taipei.veges.domain.Estimate
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.ProduceConcept
import tw.taipei.veges.domain.UnavailableReason

data class DetailUiState(
    val concept: ProduceConcept? = null,
    val selectedBasis: MarketBasis = MarketBasis.TAIPEI_COMBINED,
    val estimate: Estimate? = null,
    val unavailableReason: UnavailableReason? = null,
    val isTracked: Boolean = false,
    val methodologyExpanded: Boolean = false,
)
