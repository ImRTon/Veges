package tw.taipei.veges.alerts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import tw.taipei.veges.domain.AlertRule
import tw.taipei.veges.domain.MarketBasis

@Composable
fun AlertsRoute(
    conceptId: String? = null,
    basis: MarketBasis? = null,
    modifier: Modifier = Modifier,
) {
    val viewModel: AlertsViewModel = hiltViewModel()
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    LaunchedEffect(conceptId, basis) {
        viewModel.configureDraft(conceptId, basis)
    }
    AlertsScreen(
        rules = rules,
        draftConceptId = viewModel.draftConceptId,
        draftBasis = viewModel.draftBasis,
        thresholdInput = viewModel.thresholdInput,
        validationMessage = viewModel.validationMessage,
        onThresholdChanged = viewModel::updateThreshold,
        onBasisSelected = viewModel::selectBasis,
        onSave = viewModel::saveDraft,
        onToggle = viewModel::toggle,
        onEdit = viewModel::edit,
        onDelete = viewModel::delete,
        modifier = modifier,
    )
}

@Composable
fun AlertsScreen(
    rules: List<AlertRule>,
    draftConceptId: String,
    draftBasis: MarketBasis,
    thresholdInput: String,
    validationMessage: String,
    onThresholdChanged: (String) -> Unit,
    onBasisSelected: (MarketBasis) -> Unit,
    onSave: () -> Unit,
    onToggle: (AlertRule) -> Unit,
    onEdit: (AlertRule) -> Unit,
    onDelete: (AlertRule) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("價格提醒", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        }
        item { NotificationPermissionCard() }
        if (draftConceptId.isNotBlank()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("設定提醒", style = MaterialTheme.typography.titleLarge)
                        Text(draftConceptId, style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MarketBasis.entries.forEach { option ->
                                FilterChip(
                                    selected = option == draftBasis,
                                    onClick = { onBasisSelected(option) },
                                    label = { Text(option.label()) },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = thresholdInput,
                            onValueChange = onThresholdChanged,
                            label = { Text("低於多少元 / 台斤時提醒") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(onClick = onSave) { Text("儲存提醒") }
                        if (validationMessage.isNotBlank()) Text(validationMessage)
                    }
                }
            }
        } else {
            item {
                Text(
                    "若要新增提醒，請先從蔬果詳情頁選擇「設定提醒」。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (rules.isEmpty()) {
            item { Text("尚未建立提醒") }
        }
        items(rules, key = { it.ruleId }) { rule ->
            AlertRuleCard(rule, onToggle, onEdit, onDelete)
        }
    }
}

@Composable
private fun AlertRuleCard(
    rule: AlertRule,
    onToggle: (AlertRule) -> Unit,
    onEdit: (AlertRule) -> Unit,
    onDelete: (AlertRule) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(rule.conceptId.value, style = MaterialTheme.typography.titleMedium)
                    Text(rule.basis.label(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = rule.enabled, onCheckedChange = { onToggle(rule) })
            }
            Text(
                "估算低於 ${rule.thresholdNtdPerTaiJin.toPlainString()} / 台斤",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onEdit(rule) }) { Text("編輯") }
                TextButton(onClick = { onDelete(rule) }) { Text("刪除") }
            }
        }
    }
}

@Composable
private fun NotificationPermissionCard() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    fun notificationsGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    var granted by remember {
        mutableStateOf(notificationsGranted())
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = notificationsGranted()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (granted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (granted) "通知已啟用" else "需要通知權限", fontWeight = FontWeight.Bold)
            Text(
                if (granted) "達到門檻時可傳送本機通知。"
                else "提醒規則仍會保留，但未授權時無法顯示通知。",
            )
            if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                            )
                        },
                    ) { Text("開啟通知設定") }
                }
            }
        }
    }
}

private fun MarketBasis.label() = when (this) {
    MarketBasis.TAIPEI_COMBINED -> "台北合併"
    MarketBasis.TAIPEI_FIRST -> "台北一"
    MarketBasis.TAIPEI_SECOND -> "台北二"
}
