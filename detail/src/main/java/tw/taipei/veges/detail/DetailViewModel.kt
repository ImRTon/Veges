package tw.taipei.veges.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tw.taipei.veges.domain.DetailRepository
import tw.taipei.veges.domain.HistoryRefreshRequester
import tw.taipei.veges.domain.ItemPriceDirectionPredictor
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.MarketShockRepository
import tw.taipei.veges.domain.ProduceConceptId
import tw.taipei.veges.domain.TrackingRepository
import tw.taipei.veges.domain.TrackingUseCases
import tw.taipei.veges.domain.TrendPeriod
import tw.taipei.veges.domain.UntrackResult

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: DetailRepository,
    trackingRepository: TrackingRepository,
    private val historyRefreshRequester: HistoryRefreshRequester,
    private val marketShockRepository: MarketShockRepository,
    private val clock: Clock,
) : ViewModel() {
    private val trackingUseCases = TrackingUseCases(trackingRepository)
    private val priceDirectionPredictor = ItemPriceDirectionPredictor()
    private val mutableState = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = mutableState.asStateFlow()
    private var conceptId: ProduceConceptId? = null
    private var observationJob: Job? = null

    init {
        viewModelScope.launch { marketShockRepository.refresh() }
    }

    fun selectBasis(basis: MarketBasis) {
        mutableState.update { it.copy(selectedBasis = basis) }
        observe()
    }

    fun selectPeriod(period: TrendPeriod) {
        mutableState.update { it.copy(selectedPeriod = period) }
        if (period == TrendPeriod.ONE_YEAR) {
            conceptId?.let(historyRefreshRequester::requestOneYearHistory)
        }
        observe()
    }

    fun toggleMethodology() = mutableState.update { it.copy(methodologyExpanded = !it.methodologyExpanded) }

    fun toggleTracked() {
        val id = conceptId ?: return
        viewModelScope.launch {
            if (mutableState.value.isTracked) {
                when (val result = trackingUseCases.untrack(id, removeActiveAlerts = false)) {
                    UntrackResult.Untracked -> Unit
                    is UntrackResult.RequiresActiveAlertConfirmation ->
                        mutableState.update { it.copy(untrackConfirmationCount = result.activeAlertCount) }
                }
            } else {
                trackingUseCases.track(id, Instant.now(clock))
                historyRefreshRequester.requestOneYearHistory(id)
            }
        }
    }

    fun confirmUntrackAndRemoveAlerts() {
        val id = conceptId ?: return
        viewModelScope.launch {
            trackingUseCases.untrack(id, removeActiveAlerts = true)
            mutableState.update { it.copy(untrackConfirmationCount = null) }
        }
    }

    fun dismissUntrackConfirmation() =
        mutableState.update { it.copy(untrackConfirmationCount = null) }

    fun load(conceptId: ProduceConceptId) {
        if (this.conceptId == conceptId) return
        this.conceptId = conceptId
        historyRefreshRequester.requestOneYearHistory(conceptId)
        observe()
    }

    private fun observe() {
        val id = conceptId ?: return
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            combine(
                repository.observeDetail(
                    conceptId = id,
                    basis = mutableState.value.selectedBasis,
                    period = mutableState.value.selectedPeriod,
                ),
                marketShockRepository.signals,
            ) { snapshot, shockSignals ->
                snapshot to snapshot.concept?.let {
                    priceDirectionPredictor.evaluate(
                        history = snapshot.priceDirectionHistory,
                        shockSignals = shockSignals,
                        today = java.time.LocalDate.now(clock),
                        now = Instant.now(clock),
                    )
                }
            }.collectLatest { (snapshot, directionEvaluation) ->
                mutableState.update {
                    it.copy(
                        concept = snapshot.concept,
                        estimate = snapshot.estimate,
                        estimateHistory = snapshot.estimateHistory,
                        trendPoints = snapshot.trendPoints,
                        variantPrices = snapshot.variantPrices,
                        unavailableReason = snapshot.estimate?.unavailableReason,
                        isTracked = snapshot.isTracked,
                        priceDirectionEvaluation = directionEvaluation,
                    )
                }
            }
        }
    }
}
