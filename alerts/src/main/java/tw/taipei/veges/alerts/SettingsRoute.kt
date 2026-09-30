package tw.taipei.veges.alerts

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tw.taipei.veges.designsystem.AlertEditorSheet
import tw.taipei.veges.designsystem.PillChoiceRow
import tw.taipei.veges.designsystem.ProduceIllustration
import tw.taipei.veges.designsystem.marketLabel
import tw.taipei.veges.domain.AlertRule
import tw.taipei.veges.domain.ThemeMode

@Composable
fun SettingsRoute(modifier: Modifier = Modifier) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val alertRules by viewModel.alertRules.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsScreen(
        themeMode = themeMode,
        alertRules = alertRules,
        notificationsEnabled = rememberNotificationsEnabled(),
        onThemeModeSelected = viewModel::setThemeMode,
        onToggleAlert = viewModel::toggle,
        onEditAlert = viewModel::edit,
        onOpenNotificationSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        },
        modifier = modifier,
    )
    editor?.let { current ->
        AlertEditorSheet(
            produceName = current.item.produceName,
            currentPrice = null,
            basis = current.basis,
            thresholdInput = current.thresholdInput,
            showError = current.showError,
            isEditing = true,
            onBasisSelected = viewModel::selectBasis,
            onThresholdChanged = viewModel::updateThreshold,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
            onDismiss = viewModel::closeEditor,
        )
    }
}

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    alertRules: List<AlertRuleItem>,
    notificationsEnabled: Boolean,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onToggleAlert: (AlertRule) -> Unit,
    onEditAlert: (AlertRuleItem) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "設定",
                modifier = Modifier.padding(vertical = 4.dp),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
            )
        }

        item { SectionTitle("主題") }
        item {
            PillChoiceRow(
                items = ThemeMode.entries,
                selectedItem = themeMode,
                onItemSelected = onThemeModeSelected,
                itemLabel = ThemeMode::label,
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedContentColor = MaterialTheme.colorScheme.onPrimary,
            )
        }

        item { SectionTitle("價格提醒", Modifier.padding(top = 12.dp)) }
        if (!notificationsEnabled) {
            item {
                NotificationsOffCard(
                    hasAlerts = alertRules.any { it.rule.enabled },
                    onOpenNotificationSettings = onOpenNotificationSettings,
                )
            }
        }
        item {
            SettingsCard {
                if (alertRules.isEmpty()) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("尚未設定提醒", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "在品項頁點右上角的鈴鐺，就能設定低於某個價格時通知你。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Column {
                        alertRules.forEachIndexed { index, item ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                )
                            }
                            AlertRuleRow(
                                item = item,
                                onToggle = { onToggleAlert(item.rule) },
                                onClick = { onEditAlert(item) },
                            )
                        }
                    }
                }
            }
        }

        item { SectionTitle("資料來源與隱私", Modifier.padding(top = 12.dp)) }
        item {
            SettingsCard {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    InfoLine("價格", "農業部農產品交易行情（政府資料開放授權）")
                    InfoLine("天氣", "中央氣象署")
                    InfoLine("市場參考價", "由批發價換算的估計，並非店家實際售價。")
                    InfoLine("隱私", "不需登入。追蹤清單、提醒與設定只存在這支手機，不會上傳。")
                    Text(
                        "本 App 為獨立開發，非政府機關提供的服務。",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        content = content,
    )
}

@Composable
private fun AlertRuleRow(
    item: AlertRuleItem,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    val rule = item.rule
    val summary = "${rule.basis.marketLabel()}・低於 ${rule.thresholdNtdPerTaiJin.toPlainString()} 元 / 台斤"
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProduceIllustration(
                assetPath = item.illustrationAsset,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    item.produceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = rule.enabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.semantics {
                    contentDescription = "${item.produceName}提醒"
                    stateDescription = if (rule.enabled) "開啟" else "關閉"
                },
            )
        }
    }
}

@Composable
private fun NotificationsOffCard(
    hasAlerts: Boolean,
    onOpenNotificationSettings: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("通知已關閉", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    if (hasAlerts) "價格低於你設定的門檻時，將無法通知你。" else "開啟後才能收到價格提醒。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            FilledTonalButton(onClick = onOpenNotificationSettings) { Text("開啟") }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun rememberNotificationsEnabled(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(context.notificationsEnabled()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) enabled = context.notificationsEnabled()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return enabled
}

private fun Context.notificationsEnabled(): Boolean =
    getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() ?: true

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "跟隨系統"
    ThemeMode.LIGHT -> "淺色"
    ThemeMode.DARK -> "深色"
}
