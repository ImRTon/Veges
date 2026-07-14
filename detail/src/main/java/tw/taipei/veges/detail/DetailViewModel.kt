package tw.taipei.veges.detail

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.ProduceConceptId

@HiltViewModel
class DetailViewModel @Inject constructor() : ViewModel() {
    private val mutableState = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = mutableState.asStateFlow()

    fun selectBasis(basis: MarketBasis) = mutableState.update { it.copy(selectedBasis = basis) }

    fun toggleMethodology() = mutableState.update { it.copy(methodologyExpanded = !it.methodologyExpanded) }

    fun toggleTracked() = mutableState.update { it.copy(isTracked = !it.isTracked) }

    fun load(conceptId: ProduceConceptId) {
        // TODO(7.4): Load concept and basis-qualified estimate from the feature repository.
    }
}
