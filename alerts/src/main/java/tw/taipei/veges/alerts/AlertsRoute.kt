package tw.taipei.veges.alerts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tw.taipei.veges.domain.BACKGROUND_REFRESH_DISCLOSURE
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AlertsRoute(modifier: Modifier = Modifier) {
    val viewModel: AlertsViewModel = hiltViewModel()
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("價格提醒")
        Text("提醒固定使用建立時選定的市場基礎，不會跟隨全域設定自動改變。")
        Text(BACKGROUND_REFRESH_DISCLOSURE)
        if (rules.isEmpty()) Text("尚未建立提醒")
        rules.forEach { rule ->
            Text("${rule.conceptId.value} · ${rule.basis.label()}")
            Text("估算低於 ${rule.thresholdNtdPerTaiJin} 元 / 台斤")
            Button(onClick = { viewModel.toggle(rule) }) {
                Text(if (rule.enabled) "停用" else "啟用")
            }
        }
        OutlinedTextField(
            value = viewModel.thresholdInput,
            onValueChange = viewModel::updateThreshold,
            label = { Text("門檻（元 / 台斤）") },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(viewModel.validationMessage)
    }
}

private fun tw.taipei.veges.domain.MarketBasis.label() = when (this) {
    tw.taipei.veges.domain.MarketBasis.TAIPEI_COMBINED -> "台北合併"
    tw.taipei.veges.domain.MarketBasis.TAIPEI_FIRST -> "台北一"
    tw.taipei.veges.domain.MarketBasis.TAIPEI_SECOND -> "台北二"
}

// TODO(6.1-6.5): Add local tracking, alert rules, evaluation, and delivery.
// TODO(7.6): Add alert list/editor and notification permission remediation.
