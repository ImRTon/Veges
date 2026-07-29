package tw.taipei.veges.alerts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import tw.taipei.veges.domain.AlertRule
import tw.taipei.veges.domain.AlertRuleRepository
import tw.taipei.veges.domain.AlertRuleUseCases
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.ProduceConceptId

@HiltViewModel
class AlertsViewModel @Inject constructor(
    repository: AlertRuleRepository,
    private val clock: Clock,
) : ViewModel() {
    private val useCases = AlertRuleUseCases(repository)
    private var editingRule: AlertRule? = null
    val rules: StateFlow<List<AlertRule>> = repository.observeRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var thresholdInput by mutableStateOf("")
        private set
    var validationMessage by mutableStateOf("")
        private set
    var draftConceptId by mutableStateOf("")
        private set
    var draftBasis by mutableStateOf(MarketBasis.TAIPEI_COMBINED)
        private set

    fun updateThreshold(value: String) {
        thresholdInput = value
        validationMessage = if (value.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true) {
            "門檻有效"
        } else {
            "請輸入大於 0 的門檻"
        }
    }

    fun toggle(rule: AlertRule) {
        viewModelScope.launch {
            useCases.update(rule, rule.thresholdNtdPerTaiJin, !rule.enabled, Instant.now(clock))
        }
    }

    fun configureDraft(conceptId: String?, basis: MarketBasis?) {
        if (!conceptId.isNullOrBlank()) draftConceptId = conceptId
        if (basis != null) draftBasis = basis
    }

    fun selectBasis(basis: MarketBasis) {
        draftBasis = basis
    }

    fun edit(rule: AlertRule) {
        editingRule = rule
        draftConceptId = rule.conceptId.value
        draftBasis = rule.basis
        thresholdInput = rule.thresholdNtdPerTaiJin.toPlainString()
        validationMessage = "正在編輯提醒"
    }

    fun saveDraft() {
        val threshold = thresholdInput.toBigDecimalOrNull()
        if (draftConceptId.isBlank()) {
            validationMessage = "請先從蔬果詳情頁選擇品項"
            return
        }
        if (threshold == null || threshold <= BigDecimal.ZERO) {
            validationMessage = "請輸入大於 0 的門檻"
            return
        }
        viewModelScope.launch {
            val current = editingRule
            if (current == null) {
                useCases.create(
                    conceptId = ProduceConceptId(draftConceptId),
                    basis = draftBasis,
                    thresholdNtdPerTaiJin = threshold,
                    now = Instant.now(clock),
                )
            } else {
                useCases.update(
                    rule = current.copy(
                        conceptId = ProduceConceptId(draftConceptId),
                        basis = draftBasis,
                    ),
                    thresholdNtdPerTaiJin = threshold,
                    enabled = current.enabled,
                    now = Instant.now(clock),
                )
            }
            editingRule = null
            thresholdInput = ""
            validationMessage = "提醒已儲存"
        }
    }

    fun delete(rule: AlertRule) {
        viewModelScope.launch {
            useCases.delete(rule.ruleId)
            if (editingRule?.ruleId == rule.ruleId) {
                editingRule = null
                thresholdInput = ""
            }
        }
    }
}
