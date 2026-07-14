package tw.taipei.veges.domain

import kotlinx.coroutines.flow.Flow

data class HomeItem(
    val concept: ProduceConcept,
    val latestEstimate: Estimate?,
    val unavailableReason: UnavailableReason?,
)

interface HomeRepository {
    fun observeHome(): Flow<List<HomeItem>>

    fun requestRefresh()
}

sealed interface CatalogSearchResult {
    data object NoReviewedResult : CatalogSearchResult
    data class Single(val concept: ProduceConcept) : CatalogSearchResult
    data class Ambiguous(val query: String, val concepts: List<ProduceConcept>) : CatalogSearchResult
}
