package tw.taipei.veges.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tw.taipei.veges.domain.HomeItem
import tw.taipei.veges.domain.HomeRepository
import tw.taipei.veges.domain.MarketItem
import tw.taipei.veges.domain.MarketPriceSurgeOutlook
import tw.taipei.veges.domain.MarketShockRepository
import tw.taipei.veges.domain.PriceRefresh
import tw.taipei.veges.domain.PriceRefreshKind
import tw.taipei.veges.domain.PriceRefreshOutcome
import tw.taipei.veges.domain.PriceSurgePredictor
import tw.taipei.veges.domain.ProductionAreaWeatherRisk
import tw.taipei.veges.domain.ProductionAreaWeatherRiskEvaluator
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.ProduceRepository
import tw.taipei.veges.domain.averageChangePercent

data class HomeUiState(
    val tracked: List<HomeItem> = emptyList(),
    val decliners: List<MarketItem> = emptyList(),
    val declinerLookbackDays: Int = 7,
    val declinerEligibleCount: Int = 0,
    val marketPriceSurgeOutlook: MarketPriceSurgeOutlook? = null,
    val productionAreaWeatherRisk: ProductionAreaWeatherRisk? = null,
    val predictionEligibleCount: Int = 0,
    val priceRefresh: PriceRefresh = PriceRefresh(),
    val latestPriceDate: LocalDate? = null,
    val manualRefreshPending: Boolean = false,
)

private data class TrackedRefreshAndOrder(
    val tracked: List<HomeItem>,
    val priceRefresh: PriceRefresh,
    val manualPending: Boolean,
    val savedOrder: List<String>,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    produceRepository: ProduceRepository,
    private val marketShockRepository: MarketShockRepository,
    private val orderStore: HomeOrderStore,
) : ViewModel() {
    private val declinerLookbackDays = MutableStateFlow(7)
    private val predictor = PriceSurgePredictor()
    private val refreshNotices = Channel<PriceRefreshOutcome>(Channel.BUFFERED)
    val refreshNotice = refreshNotices.receiveAsFlow()
    private var manualRefreshAfterVersion: Long? = null
    private var manualRefreshTimeoutJob: Job? = null
    private val manualRefreshPending = MutableStateFlow(false)
    private val productionAreaWeatherRiskEvaluator = ProductionAreaWeatherRiskEvaluator()
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
        manualRefreshPending,
    ) { tracked, priceRefresh, manualPending ->
        Triple(tracked, priceRefresh, manualPending)
    }
    private val savedOrder = MutableStateFlow(orderStore.load())
    private val trackedRefreshAndOrder = combine(trackedAndRefresh, savedOrder) {
        (tracked, priceRefresh, manualPending), savedOrder ->
        TrackedRefreshAndOrder(tracked, priceRefresh, manualPending, savedOrder)
    }

    val state: StateFlow<HomeUiState> = combine(
        trackedRefreshAndOrder,
        vegetableMarket,
        allProduceMarket,
        declinerLookbackDays,
        marketShockRepository.signals,
    ) { (tracked, priceRefresh, manualPending, savedOrder), vegetables, allProduce, lookbackDays, shockSignals ->
        val ranked = vegetables.mapNotNull { item ->
            item.averageChangePercent(lookbackDays)?.let { change -> item to change }
        }
        val predictionEvaluation = predictor.evaluate(allProduce, shockSignals)
        val productionAreaWeatherRisk = productionAreaWeatherRiskEvaluator.evaluate(shockSignals)
        HomeUiState(
            tracked = tracked.orderedBySavedIds(savedOrder),
            decliners = ranked
                .filter { (_, change) -> change.signum() < 0 }
                .sortedBy { (_, change) -> change }
                .take(10)
                .map { (item, _) -> item },
            declinerLookbackDays = lookbackDays,
            declinerEligibleCount = ranked.size,
            marketPriceSurgeOutlook = predictionEvaluation.marketOutlook,
            productionAreaWeatherRisk = productionAreaWeatherRisk,
            predictionEligibleCount = predictionEvaluation.eligibleItemCount,
            priceRefresh = priceRefresh,
            manualRefreshPending = manualPending,
            latestPriceDate = allProduce.asSequence()
                .mapNotNull { item ->
                    item.latestEstimate?.takeIf { it.point != null }
                        ?.wholesaleSourceDates?.maxOrNull()
                }
                .maxOrNull(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            repository.observePriceRefresh().collect { refresh ->
                val requestedAfter = manualRefreshAfterVersion ?: return@collect
                if (!refresh.isRunning && refresh.kind == PriceRefreshKind.LATEST &&
                    refresh.completionVersion > requestedAfter
                ) {
                    manualRefreshAfterVersion = null
                    manualRefreshPending.value = false
                    manualRefreshTimeoutJob?.cancel()
                    refresh.outcome?.let { refreshNotices.send(it) }
                }
            }
        }
        viewModelScope.launch { marketShockRepository.refresh() }
    }

    fun refresh() {
        if (manualRefreshAfterVersion != null) return
        manualRefreshAfterVersion = repository.observePriceRefresh().value.completionVersion
        manualRefreshPending.value = true
        manualRefreshTimeoutJob?.cancel()
        manualRefreshTimeoutJob = viewModelScope.launch {
            delay(MANUAL_REFRESH_WAIT_LIMIT_MILLIS)
            if (manualRefreshAfterVersion != null) {
                manualRefreshAfterVersion = null
                manualRefreshPending.value = false
                refreshNotices.send(PriceRefreshOutcome.FAILED)
            }
        }
        viewModelScope.launch {
            val enqueued = try {
                repository.requestRefresh()
            } catch (failure: Exception) {
                false
            }
            if (!enqueued) {
                manualRefreshAfterVersion = null
                manualRefreshPending.value = false
                manualRefreshTimeoutJob?.cancel()
                refreshNotices.send(PriceRefreshOutcome.FAILED)
            }
        }
        viewModelScope.launch { marketShockRepository.refresh() }
    }

    fun setDeclinerLookbackDays(value: Int) {
        if (value in DECLINER_LOOKBACK_OPTIONS) {
            declinerLookbackDays.value = value
        }
    }

    fun moveTrackedItem(draggedConceptId: String, targetConceptId: String) {
        val reorderedIds = state.value.tracked
            .map { it.concept.id.value }
            .move(draggedConceptId, targetConceptId)
            ?: return
        orderStore.save(reorderedIds)
        savedOrder.value = reorderedIds
    }

    private companion object {
        val DECLINER_LOOKBACK_OPTIONS = setOf(1, 3, 7, 14, 30)
        const val MANUAL_REFRESH_WAIT_LIMIT_MILLIS = 30_000L
    }
}

class HomeOrderStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): List<String> = preferences.getString(ORDER_KEY, null)
        ?.lineSequence()
        ?.filter(String::isNotBlank)
        ?.toList()
        .orEmpty()

    fun save(conceptIds: List<String>) {
        preferences.edit()
            .putString(ORDER_KEY, conceptIds.joinToString("\n"))
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "home-order"
        const val ORDER_KEY = "tracked-concepts"
    }
}

internal fun List<HomeItem>.orderedBySavedIds(savedIds: List<String>): List<HomeItem> {
    if (savedIds.isEmpty()) return this
    val order = savedIds.withIndex().associate { (index, id) -> id to index }
    return withIndex()
        .sortedWith(
            compareBy<IndexedValue<HomeItem>>(
                { order[it.value.concept.id.value] ?: Int.MAX_VALUE },
                IndexedValue<HomeItem>::index,
            ),
        )
        .map(IndexedValue<HomeItem>::value)
}

internal fun List<String>.move(draggedId: String, targetId: String): List<String>? {
    val fromIndex = indexOf(draggedId)
    val targetIndex = indexOf(targetId)
    if (fromIndex == -1 || targetIndex == -1 || fromIndex == targetIndex) return null
    return toMutableList().apply {
        add(targetIndex, removeAt(fromIndex))
    }
}
