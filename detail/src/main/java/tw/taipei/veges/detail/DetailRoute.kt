package tw.taipei.veges.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.math.RoundingMode
import tw.taipei.veges.designsystem.AlertEditorSheet
import tw.taipei.veges.designsystem.PillChoiceRow
import tw.taipei.veges.designsystem.produceContainerTransform
import tw.taipei.veges.designsystem.produceSharedIllustration
import tw.taipei.veges.designsystem.produceSharedName
import tw.taipei.veges.designsystem.ProduceIllustration
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.ProduceConceptId
import tw.taipei.veges.domain.TrendPeriod
import tw.taipei.veges.domain.VariantMarketPrice

@Composable
fun DetailRoute(
    conceptId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    previewName: String? = null,
    previewIllustrationAsset: String? = null,
    onAlertSaved: () -> Unit = {},
) {
    val viewModel: DetailViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(conceptId) {
        viewModel.load(ProduceConceptId(conceptId))
    }
    DetailScreen(
        state = state,
        onBasisSelected = viewModel::selectBasis,
        onPeriodSelected = viewModel::selectPeriod,
        onToggleTracked = viewModel::toggleTracked,
        onSetAlert = viewModel::openAlertEditor,
        onToggleMethodology = viewModel::toggleMethodology,
        onConfirmUntrack = viewModel::confirmUntrackAndRemoveAlerts,
        onDismissUntrack = viewModel::dismissUntrackConfirmation,
        onBack = onBack,
        conceptId = conceptId,
        previewName = previewName,
        previewIllustrationAsset = previewIllustrationAsset,
        modifier = modifier,
    )
    state.alertEditor?.let { editor ->
        AlertEditorSheet(
            produceName = state.concept?.householdName ?: previewName ?: "這項蔬果",
            currentPrice = state.estimate?.point?.amount?.takeIf { editor.basis == state.selectedBasis },
            basis = editor.basis,
            thresholdInput = editor.thresholdInput,
            showError = editor.showError,
            isEditing = editor.editingRule != null,
            onBasisSelected = viewModel::selectAlertBasis,
            onThresholdChanged = viewModel::updateAlertThreshold,
            onSave = { if (viewModel.saveAlert()) onAlertSaved() },
            onDelete = viewModel::deleteAlert,
            onDismiss = viewModel::closeAlertEditor,
        )
    }
}

@Composable
fun DetailScreen(
    state: DetailUiState,
    onBasisSelected: (MarketBasis) -> Unit,
    onToggleTracked: () -> Unit,
    onToggleMethodology: () -> Unit,
    modifier: Modifier = Modifier,
    onPeriodSelected: (TrendPeriod) -> Unit = {},
    onSetAlert: () -> Unit = {},
    onConfirmUntrack: () -> Unit = {},
    onDismissUntrack: () -> Unit = {},
    onBack: () -> Unit = {},
    conceptId: String? = null,
    previewName: String? = null,
    previewIllustrationAsset: String? = null,
) {
    state.untrackConfirmationCount?.let { count ->
        AlertDialog(
            onDismissRequest = onDismissUntrack,
            title = { Text("取消追蹤？") },
            text = { Text("這個品項有 $count 個啟用提醒。若繼續，這些提醒也會一併移除。") },
            confirmButton = {
                TextButton(onClick = onConfirmUntrack) { Text("移除追蹤與提醒") }
            },
            dismissButton = {
                TextButton(onClick = onDismissUntrack) { Text("保留") }
            },
        )
    }
    val displayName = state.concept?.householdName ?: previewName
    Surface(
        modifier = modifier
            .then(if (conceptId != null) Modifier.produceContainerTransform(conceptId) else Modifier)
            .fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        displayName ?: "蔬果詳情",
                        modifier = if (conceptId != null) Modifier.produceSharedName(conceptId) else Modifier,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                    )
                    if (displayName == null) {
                        Text(
                            "正在載入行情",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onSetAlert) {
                    Icon(
                        if (state.hasActiveAlert) Icons.Rounded.Notifications else Icons.Rounded.NotificationsNone,
                        contentDescription = if (state.hasActiveAlert) "編輯提醒" else "設定提醒",
                        tint = if (state.hasActiveAlert) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                IconButton(onClick = onToggleTracked) {
                    Icon(
                        if (state.isTracked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = if (state.isTracked) "取消追蹤" else "加入追蹤",
                        tint = if (state.isTracked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            ProduceIllustration(
                assetPath = state.concept?.illustrationAsset ?: previewIllustrationAsset,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .then(
                        if (conceptId != null) {
                            Modifier.produceSharedIllustration(conceptId, MaterialTheme.shapes.extraLarge)
                        } else {
                            Modifier
                        },
                    )
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
            )
            PillChoiceRow(
                items = MarketBasis.entries,
                selectedItem = state.selectedBasis,
                onItemSelected = onBasisSelected,
                itemLabel = MarketBasis::displayName,
            )
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val point = state.estimate?.point
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("市場參考", style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    point?.amount?.toPlainString() ?: "—",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Black,
                                )
                                Text(
                                    " / 台斤",
                                    modifier = Modifier.padding(bottom = 7.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (state.estimate == null || point == null) {
                        Text(
                            "目前無可用估算",
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        val sourceDates = state.estimate.wholesaleSourceDates
                            .distinct()
                            .sorted()
                            .joinToString("、") { "${it.monthValue}/${it.dayOfMonth}" }
                        if (sourceDates.isNotEmpty()) {
                            Text(
                                "$sourceDates 批發行情",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            ItemPriceDirectionRadarCard(
                householdName = state.concept?.householdName ?: "這項蔬果",
                basis = state.selectedBasis,
                evaluation = state.priceDirectionEvaluation,
            )
            if (
                state.concept?.category == ProduceCategory.FRUIT &&
                state.variantPrices.size > 1
            ) {
                VariantPriceSection(
                    householdName = state.concept.householdName,
                    prices = state.variantPrices,
                )
            }
            WholesaleTrendChart(
                points = state.trendPoints,
                estimateHistory = state.estimateHistory,
                period = state.selectedPeriod,
                onPeriodSelected = onPeriodSelected,
            )
            HorizontalDivider()
            OutlinedButton(onClick = onToggleMethodology) {
                Text(if (state.methodologyExpanded) "收合方法與來源" else "方法與來源")
            }
            if (state.methodologyExpanded) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("資料說明", style = MaterialTheme.typography.titleMedium)
                        Text("估算公式：批發平均價（元/公斤）× 0.6 公斤/台斤 × 2.0")
                        Text(
                            "台北合併：台北一、台北二同日有效資料依成交量加權。",
                        )
                        Text(
                            "K 線：7 日與 30 日每根為一個交易日，90 日與 1 年每根為一週；" +
                                "線體連接前一根與這一根的平均價，影線為官方高低價；價格單位皆為元/台斤。",
                        )
                        Text(
                            "資料來源：農業部農產品交易行情。",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VariantPriceSection(
    householdName: String,
    prices: List<VariantMarketPrice>,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "品種批發行情",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            prices.forEachIndexed { index, price ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            price.officialName.variantDisplayName(householdName),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            price.observedOn?.let {
                                "行情日期 ${it.year}/${it.monthValue}/${it.dayOfMonth}"
                            } ?: "目前無行情",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            price.wholesaleAverage?.amount
                                ?.setScale(1, RoundingMode.HALF_UP)
                                ?.toPlainString()
                                ?: "—",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            "批發均價 / 台斤",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun String.variantDisplayName(householdName: String): String {
    val prefix = "$householdName-"
    return removePrefix(prefix).ifBlank { this }
}

private fun MarketBasis.displayName(): String = when (this) {
    MarketBasis.TAIPEI_COMBINED -> "台北合併"
    MarketBasis.TAIPEI_FIRST -> "台北一"
    MarketBasis.TAIPEI_SECOND -> "台北二"
}
