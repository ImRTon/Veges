package tw.taipei.veges.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.Normalizer
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tw.taipei.veges.domain.CatalogSearchResult
import tw.taipei.veges.domain.MarketItem
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.ProduceConcept
import tw.taipei.veges.domain.ProduceRepository

data class CatalogUiState(
    val query: String = "",
    val category: ProduceCategory = ProduceCategory.VEGETABLE,
    val results: List<MarketItem> = emptyList(),
    val ambiguity: CatalogSearchResult.Ambiguous? = null,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val repository: ProduceRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val category = MutableStateFlow(ProduceCategory.VEGETABLE)
    private val dismissedAmbiguity = MutableStateFlow<String?>(null)

    val state: StateFlow<CatalogUiState> =
        combine(query, category, dismissedAmbiguity) { text, selectedCategory, dismissed ->
            SearchRequest(text, selectedCategory, dismissed)
        }.flatMapLatest { request ->
            repository.observeMarket(request.category).map { market ->
                val normalizedQuery = normalize(request.query)
                val results = if (normalizedQuery.isBlank()) {
                    market
                } else {
                    market.filter { it.concept.matches(normalizedQuery) }
                }
                val exactMatches = if (normalizedQuery.isBlank()) {
                    emptyList()
                } else {
                    results.map(MarketItem::concept)
                        .filter { it.hasExactIdentity(normalizedQuery) }
                }
                CatalogUiState(
                    query = request.query,
                    category = request.category,
                    results = results,
                    ambiguity = if (
                        exactMatches.size > 1 &&
                        request.dismissedAmbiguity != normalizedQuery
                    ) {
                        CatalogSearchResult.Ambiguous(request.query, exactMatches)
                    } else {
                        null
                    },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())

    fun updateQuery(value: String) {
        dismissedAmbiguity.value = null
        query.value = value
    }

    fun selectCategory(value: ProduceCategory) {
        dismissedAmbiguity.value = null
        category.value = value
    }

    fun clearAmbiguity() {
        dismissedAmbiguity.value = normalize(query.value)
    }

    private data class SearchRequest(
        val query: String,
        val category: ProduceCategory,
        val dismissedAmbiguity: String?,
    )
}

private fun ProduceConcept.hasExactIdentity(normalizedQuery: String): Boolean =
    normalize(householdName) == normalizedQuery ||
        aliases.any { normalize(it) == normalizedQuery }

private fun ProduceConcept.matches(normalizedQuery: String): Boolean =
    normalize(householdName).contains(normalizedQuery) ||
        aliases.any { normalize(it).contains(normalizedQuery) } ||
        officialVariants.any {
            normalize(it.officialName).contains(normalizedQuery) ||
                normalize(it.code.value).contains(normalizedQuery)
        }

private fun normalize(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase()
        .filterNot(Char::isWhitespace)
