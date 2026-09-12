package tw.taipei.veges.catalog

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.Normalizer
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
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
    private val orderStore: CatalogOrderStore,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val category = MutableStateFlow(ProduceCategory.VEGETABLE)
    private val dismissedAmbiguity = MutableStateFlow<String?>(null)
    private val orderByCategory = MutableStateFlow(
        ProduceCategory.entries.associateWith(orderStore::load),
    )

    private val market: StateFlow<MarketSnapshot> =
        category
            .flatMapLatest { selectedCategory ->
                repository.observeMarket(selectedCategory)
                    .map { items -> MarketSnapshot(selectedCategory, items) }
                    .flowOn(Dispatchers.Default)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                MarketSnapshot(ProduceCategory.VEGETABLE, emptyList()),
            )

    private val request = combine(query, category, dismissedAmbiguity, orderByCategory) {
                text,
                selectedCategory,
                dismissed,
                orders,
            ->
            SearchRequest(
                query = text,
                category = selectedCategory,
                dismissedAmbiguity = dismissed,
                orderedConceptIds = orders[selectedCategory].orEmpty(),
            )
        }

    val state: StateFlow<CatalogUiState> = combine(request, market) { searchRequest, snapshot ->
        val availableMarket = snapshot.items.takeIf { snapshot.category == searchRequest.category }
            .orEmpty()
        val normalizedQuery = normalize(searchRequest.query)
        val orderedMarket = availableMarket.orderedByConceptIds(searchRequest.orderedConceptIds)
        val results = if (normalizedQuery.isBlank()) {
            orderedMarket
        } else {
            orderedMarket.filter { it.concept.matches(normalizedQuery) }
        }
        val exactMatches = if (normalizedQuery.isBlank()) {
            emptyList()
        } else {
            results.map(MarketItem::concept)
                .filter { it.hasExactIdentity(normalizedQuery) }
        }
        CatalogUiState(
            query = searchRequest.query,
            category = searchRequest.category,
            results = results,
            ambiguity = if (
                exactMatches.size > 1 &&
                searchRequest.dismissedAmbiguity != normalizedQuery
            ) {
                CatalogSearchResult.Ambiguous(searchRequest.query, exactMatches)
            } else {
                null
            }
        )
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

    fun moveItem(draggedConceptId: String, targetConceptId: String) {
        if (query.value.isNotBlank() || draggedConceptId == targetConceptId) return
        val currentCategory = category.value
        val currentIds = state.value.results
            .map { it.concept.id.value }
            .orderedBySavedIds(orderByCategory.value[currentCategory].orEmpty())
        val reorderedIds = currentIds
            .move(draggedConceptId, targetConceptId)
        if (reorderedIds == null) return

        orderByCategory.value = orderByCategory.value + (currentCategory to reorderedIds)
        orderStore.save(currentCategory, reorderedIds)
    }

    private data class SearchRequest(
        val query: String,
        val category: ProduceCategory,
        val dismissedAmbiguity: String?,
        val orderedConceptIds: List<String>,
    )

    private data class MarketSnapshot(
        val category: ProduceCategory,
        val items: List<MarketItem>,
    )
}

class CatalogOrderStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(category: ProduceCategory): List<String> =
        preferences.getString(category.preferenceKey(), null)
            ?.lineSequence()
            ?.filter(String::isNotBlank)
            ?.toList()
            .orEmpty()

    fun save(category: ProduceCategory, conceptIds: List<String>) {
        preferences.edit()
            .putString(category.preferenceKey(), conceptIds.joinToString("\n"))
            .apply()
    }

    private fun ProduceCategory.preferenceKey(): String = "order-${name.lowercase()}"

    private companion object {
        const val PREFERENCES_NAME = "catalog-order"
    }
}

internal fun List<MarketItem>.orderedByConceptIds(conceptIds: List<String>): List<MarketItem> {
    if (conceptIds.isEmpty()) return this
    val order = conceptIds.withIndex().associate { (index, id) -> id to index }
    return withIndex()
        .sortedWith(
            compareBy<IndexedValue<MarketItem>>(
                { order[it.value.concept.id.value] ?: Int.MAX_VALUE },
                IndexedValue<MarketItem>::index,
            ),
        )
        .map(IndexedValue<MarketItem>::value)
}

internal fun List<String>.move(draggedId: String, targetId: String): List<String>? {
    val fromIndex = indexOf(draggedId)
    val targetIndex = indexOf(targetId)
    if (fromIndex == -1 || targetIndex == -1 || fromIndex == targetIndex) return null
    return toMutableList().apply {
        add(targetIndex, removeAt(fromIndex))
    }
}

private fun List<String>.orderedBySavedIds(savedIds: List<String>): List<String> {
    if (savedIds.isEmpty()) return this
    val order = savedIds.withIndex().associate { (index, id) -> id to index }
    return withIndex()
        .sortedWith(
            compareBy<IndexedValue<String>>(
                { order[it.value] ?: Int.MAX_VALUE },
                IndexedValue<String>::index,
            ),
        )
        .map(IndexedValue<String>::value)
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
