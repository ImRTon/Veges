package tw.taipei.veges.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tw.taipei.veges.designsystem.parseAlertThreshold
import tw.taipei.veges.domain.AlertRule
import tw.taipei.veges.domain.AlertRuleRepository
import tw.taipei.veges.domain.AlertRuleUseCases
import tw.taipei.veges.domain.AppearanceRepository
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.ProduceRepository
import tw.taipei.veges.domain.ThemeMode

data class AlertRuleItem(
    val rule: AlertRule,
    val produceName: String,
    val illustrationAsset: String?,
)

data class SettingsAlertEditor(
    val item: AlertRuleItem,
    val basis: MarketBasis,
    val thresholdInput: String,
    val showError: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val alertRuleRepository: AlertRuleRepository,
    produceRepository: ProduceRepository,
    private val appearanceRepository: AppearanceRepository,
    private val clock: Clock,
) : ViewModel() {
    private val useCases = AlertRuleUseCases(alertRuleRepository)

    val themeMode: StateFlow<ThemeMode> = appearanceRepository.themeMode

    val alertRules: StateFlow<List<AlertRuleItem>> = combine(
        alertRuleRepository.observeRules(),
        produceRepository.browse(ProduceCategory.VEGETABLE),
        produceRepository.browse(ProduceCategory.FRUIT),
    ) { rules, vegetables, fruits ->
        val concepts = (vegetables + fruits).associateBy { it.id }
        rules.map { rule ->
            val concept = concepts[rule.conceptId]
            AlertRuleItem(
                rule = rule,
                produceName = concept?.householdName ?: "未知品項",
                illustrationAsset = concept?.illustrationAsset,
            )
        }.sortedBy { it.produceName }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val mutableEditor = MutableStateFlow<SettingsAlertEditor?>(null)
    val editor: StateFlow<SettingsAlertEditor?> = mutableEditor.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = appearanceRepository.setThemeMode(mode)

    fun toggle(rule: AlertRule) {
        viewModelScope.launch {
            useCases.update(rule, rule.thresholdNtdPerTaiJin, !rule.enabled, Instant.now(clock))
        }
    }

    fun edit(item: AlertRuleItem) {
        mutableEditor.value = SettingsAlertEditor(
            item = item,
            basis = item.rule.basis,
            thresholdInput = item.rule.thresholdNtdPerTaiJin.toPlainString(),
        )
    }

    fun selectBasis(basis: MarketBasis) = mutableEditor.update { it?.copy(basis = basis) }

    fun updateThreshold(input: String) =
        mutableEditor.update { it?.copy(thresholdInput = input, showError = false) }

    fun closeEditor() {
        mutableEditor.value = null
    }

    fun save() {
        val editor = mutableEditor.value ?: return
        val threshold = parseAlertThreshold(editor.thresholdInput)
        if (threshold == null) {
            mutableEditor.value = editor.copy(showError = true)
            return
        }
        mutableEditor.value = null
        viewModelScope.launch {
            useCases.update(
                rule = editor.item.rule.copy(basis = editor.basis),
                thresholdNtdPerTaiJin = threshold,
                enabled = editor.item.rule.enabled,
                now = Instant.now(clock),
            )
        }
    }

    fun delete() {
        val editor = mutableEditor.value ?: return
        mutableEditor.value = null
        viewModelScope.launch { useCases.delete(editor.item.rule.ruleId) }
    }
}
