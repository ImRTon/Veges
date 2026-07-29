package tw.taipei.veges.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import tw.taipei.veges.domain.HomeItem
import tw.taipei.veges.domain.HomeRepository
import tw.taipei.veges.domain.MarketItem
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.ProduceRepository
import tw.taipei.veges.domain.averageChangePercent

data class HomeUiState(
    val tracked: List<HomeItem> = emptyList(),
    val decliners: List<MarketItem> = emptyList(),
    val declinerLookbackDays: Int = 7,
    val declinerEligibleCount: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    produceRepository: ProduceRepository,
) : ViewModel() {
    private val declinerLookbackDays = MutableStateFlow(7)

    val state: StateFlow<HomeUiState> = combine(
        repository.observeHome(),
        produceRepository.observeMarket(ProduceCategory.VEGETABLE),
        declinerLookbackDays,
    ) { tracked, market, lookbackDays ->
        val ranked = market.mapNotNull { item ->
            item.averageChangePercent(lookbackDays)?.let { change -> item to change }
        }
        HomeUiState(
            tracked = tracked,
            decliners = ranked
                .filter { (_, change) -> change.signum() < 0 }
                .sortedBy { (_, change) -> change }
                .take(10)
                .map { (item, _) -> item },
            declinerLookbackDays = lookbackDays,
            declinerEligibleCount = ranked.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refresh() {
        repository.requestRefresh()
    }

    fun setDeclinerLookbackDays(value: Int) {
        if (value in DECLINER_LOOKBACK_OPTIONS) {
            declinerLookbackDays.value = value
        }
    }

    private companion object {
        val DECLINER_LOOKBACK_OPTIONS = setOf(1, 3, 7, 14, 30)
    }
}
