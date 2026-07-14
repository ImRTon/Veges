package tw.taipei.veges.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import tw.taipei.veges.domain.CatalogSearchResult
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.ProduceRepository

data class CatalogUiState(
    val query: String = "",
    val category: ProduceCategory = ProduceCategory.VEGETABLE,
    val results: List<tw.taipei.veges.domain.ProduceConcept> = emptyList(),
    val ambiguity: CatalogSearchResult.Ambiguous? = null,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val repository: ProduceRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val category = MutableStateFlow(ProduceCategory.VEGETABLE)

    val state: StateFlow<CatalogUiState> = kotlinx.coroutines.flow.combine(query, category) { text, selectedCategory ->
        text to selectedCategory
    }.flatMapLatest { (text, selectedCategory) ->
            if (text.isBlank()) repository.browse(selectedCategory)
            else repository.search(text)
        }
        .flatMapLatest { results ->
            kotlinx.coroutines.flow.flow {
                emit(
                    CatalogUiState(
                        query = query.value,
                        category = category.value,
                        results = results,
                        ambiguity = if (results.size > 1 && query.value.isNotBlank()) {
                            CatalogSearchResult.Ambiguous(query.value, results)
                        } else null,
                    ),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())

    fun updateQuery(value: String) = query.update { value }

    fun selectCategory(value: ProduceCategory) = category.update { value }

    fun clearAmbiguity() {
        // The choice sheet is intentionally closed by the UI only after explicit selection.
    }
}
