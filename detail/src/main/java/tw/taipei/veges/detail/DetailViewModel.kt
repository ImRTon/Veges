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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tw.taipei.veges.designsystem.parseAlertThreshold
import tw.taipei.veges.domain.AlertRule
import tw.taipei.veges.domain.AlertRuleRepository
import tw.taipei.veges.domain.AlertRuleUseCases
import tw.taipei.veges.domain.DetailRepository
import tw.taipei.veges.domain.HistoryRefreshRequester
import tw.taipei.veges.domain.ItemPriceDirectionPredictor
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.ProduceConceptId
import tw.taipei.veges.domain.TrackingRepository
import tw.taipei.veges.domain.TrackingUseCases
import tw.taipei.veges.domain.TrendPeriod
import tw.taipei.veges.domain.UntrackResult

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: DetailRepository,
    trackingRepository: TrackingRepository,
    private val alertRuleRepository: AlertRuleRepository,
    private val historyRefreshRequester: HistoryRefreshRequester,
    private val clock: Clock,
) : ViewModel() {
    private val trackingUseCases = TrackingUseCases(trackingRepository)
    private val alertRuleUseCases = AlertRuleUseCases(alertRuleRepository)
    private var conceptAlertRules: List<AlertRule> = emptyList()
    private var alertRulesJob: Job? = null
    private val priceDirectionPredictor = ItemPriceDirectionPredictor()
    private val mutableState = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = mutableState.asStateFlow()
    private var conceptId: ProduceConceptId? = null
    private var observationJob: Job? = null

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
        observeAlertRules(conceptId)
    }

    fun openAlertEditor() {
        val selectedBasis = mutableState.value.selectedBasis
        val existing = conceptAlertRules.firstOrNull { it.basis == selectedBasis }
            ?: conceptAlertRules.firstOrNull()
        mutableState.update {
            it.copy(
                alertEditor = AlertEditorState(
                    basis = existing?.basis ?: selectedBasis,
                    thresholdInput = existing?.thresholdNtdPerTaiJin?.toPlainString().orEmpty(),
                    editingRule = existing,
                ),
            )
        }
    }

    fun selectAlertBasis(basis: MarketBasis) = mutableState.update { state ->
        state.copy(alertEditor = state.alertEditor?.copy(basis = basis))
    }

    fun updateAlertThreshold(input: String) = mutableState.update { state ->
        state.copy(alertEditor = state.alertEditor?.copy(thresholdInput = input, showError = false))
    }

    fun closeAlertEditor() = mutableState.update { it.copy(alertEditor = null) }

    /** Returns true when the alert was valid and is being saved. */
    fun saveAlert(): Boolean {
        val id = conceptId ?: return false
        val editor = mutableState.value.alertEditor ?: return false
        val threshold = parseAlertThreshold(editor.thresholdInput)
        if (threshold == null) {
            mutableState.update { it.copy(alertEditor = editor.copy(showError = true)) }
            return false
        }
        mutableState.update { it.copy(alertEditor = null) }
        viewModelScope.launch {
            val now = Instant.now(clock)
            val existing = editor.editingRule
            if (existing == null) {
                alertRuleUseCases.create(id, editor.basis, threshold, now)
            } else {
                alertRuleUseCases.update(
                    rule = existing.copy(basis = editor.basis),
                    thresholdNtdPerTaiJin = threshold,
                    enabled = true,
                    now = now,
                )
            }
        }
        return true
    }

    fun deleteAlert() {
        val rule = mutableState.value.alertEditor?.editingRule
        mutableState.update { it.copy(alertEditor = null) }
        if (rule != null) {
            viewModelScope.launch { alertRuleUseCases.delete(rule.ruleId) }
        }
    }

    private fun observeAlertRules(id: ProduceConceptId) {
        alertRulesJob?.cancel()
        alertRulesJob = viewModelScope.launch {
            alertRuleRepository.observeRules().collectLatest { rules ->
                conceptAlertRules = rules.filter { it.conceptId == id }
                mutableState.update { state ->
                    state.copy(hasActiveAlert = conceptAlertRules.any(AlertRule::enabled))
                }
            }
        }
    }

    private fun observe() {
        val id = conceptId ?: return
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            repository.observeDetail(
                conceptId = id,
                basis = mutableState.value.selectedBasis,
                period = mutableState.value.selectedPeriod,
            ).collectLatest { snapshot ->
                val directionEvaluation = snapshot.concept?.let {
                    priceDirectionPredictor.evaluate(
                        history = snapshot.priceDirectionHistory,
                        today = java.time.LocalDate.now(clock),
                    )
                }
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
