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
import kotlinx.coroutines.launch
import tw.taipei.veges.domain.HomeItem
import tw.taipei.veges.domain.HomeRepository
import tw.taipei.veges.domain.MarketItem
import tw.taipei.veges.domain.MarketPriceSurgeOutlook
import tw.taipei.veges.domain.MarketShockRepository
import tw.taipei.veges.domain.PriceRefresh
import tw.taipei.veges.domain.PriceSurgePredictor
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.ProduceRepository
import tw.taipei.veges.domain.averageChangePercent

data class HomeUiState(
    val tracked: List<HomeItem> = emptyList(),
    val decliners: List<MarketItem> = emptyList(),
    val declinerLookbackDays: Int = 7,
    val declinerEligibleCount: Int = 0,
    val marketPriceSurgeOutlook: MarketPriceSurgeOutlook? = null,
    val predictionEligibleCount: Int = 0,
    val priceRefresh: PriceRefresh = PriceRefresh(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    produceRepository: ProduceRepository,
    private val marketShockRepository: MarketShockRepository,
) : ViewModel() {
    private val declinerLookbackDays = MutableStateFlow(7)
    private val predictor = PriceSurgePredictor()
    private val vegetableMarket = produceRepository.observeMarket(ProduceCategory.VEGETABLE)
    private val allProduceMarket = combine(
        vegetableMarket,
        produceRepository.observeMarket(ProduceCategory.FRUIT),
    ) { vegetables, fruit ->
        vegetables + fruit
    }
    private val trackedAndRefresh = combine(
        repository.observeHome(),
        repository.observePriceRefresh(),
    ) { tracked, priceRefresh ->
        tracked to priceRefresh
    }

    val state: StateFlow<HomeUiState> = combine(
        trackedAndRefresh,
        vegetableMarket,
        allProduceMarket,
        declinerLookbackDays,
        marketShockRepository.signals,
    ) { (tracked, priceRefresh), vegetables, allProduce, lookbackDays, shockSignals ->
        val ranked = vegetables.mapNotNull { item ->
            item.averageChangePercent(lookbackDays)?.let { change -> item to change }
        }
        val predictionEvaluation = predictor.evaluate(allProduce, shockSignals)
        HomeUiState(
            tracked = tracked,
            decliners = ranked
                .filter { (_, change) -> change.signum() < 0 }
                .sortedBy { (_, change) -> change }
                .take(10)
                .map { (item, _) -> item },
            declinerLookbackDays = lookbackDays,
            declinerEligibleCount = ranked.size,
            marketPriceSurgeOutlook = predictionEvaluation.marketOutlook,
            predictionEligibleCount = predictionEvaluation.eligibleItemCount,
            priceRefresh = priceRefresh,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch { marketShockRepository.refresh() }
    }

    fun refresh() {
        repository.requestRefresh()
        viewModelScope.launch { marketShockRepository.refresh() }
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
