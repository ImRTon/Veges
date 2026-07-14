package tw.taipei.veges.alerts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import tw.taipei.veges.domain.AlertRule
import tw.taipei.veges.domain.AlertRuleRepository
import tw.taipei.veges.domain.AlertRuleUseCases

@HiltViewModel
class AlertsViewModel @Inject constructor(
    repository: AlertRuleRepository,
) : ViewModel() {
    private val useCases = AlertRuleUseCases(repository)
    val rules: StateFlow<List<AlertRule>> = repository.observeRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var thresholdInput by mutableStateOf("")
        private set
    var validationMessage by mutableStateOf("")
        private set

    fun updateThreshold(value: String) {
        thresholdInput = value
        validationMessage = if (value.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true) {
            "門檻有效；指標為 Taipei retail reference estimate（估算）"
        } else {
            "請輸入大於 0 的門檻"
        }
    }

    fun toggle(rule: AlertRule) {
        viewModelScope.launch {
            useCases.update(rule, rule.thresholdNtdPerTaiJin, !rule.enabled, java.time.Instant.now())
        }
    }
}
