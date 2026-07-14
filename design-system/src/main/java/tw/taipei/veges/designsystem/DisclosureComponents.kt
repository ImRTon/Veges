package tw.taipei.veges.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import tw.taipei.veges.domain.EstimateDisclosure

@Composable
fun AiIllustrationDisclosure(modifier: Modifier = Modifier) {
    Text(
        text = "AI 生成示意圖，非實物照片。",
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier.semantics {
            contentDescription = "AI 生成示意圖，非實物照片。不可作為產地、等級、認證、包裝或實物證據。"
        },
    )
}

@Composable
fun UnavailableState(
    reason: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(24.dp)) {
        Text("目前無法提供估價", style = MaterialTheme.typography.titleMedium)
        Text(reason, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun AsyncStateLabel(
    state: UiState<*>,
    modifier: Modifier = Modifier,
) {
    val label = when (state) {
        UiState.Loading -> "載入中"
        is UiState.Refreshing<*> -> "更新中，仍顯示已快取資料"
        is UiState.Current<*> -> "資料為最新可用狀態"
        is UiState.Stale<*> -> "資料較舊，來源日期 ${state.sourceDate}"
        is UiState.Offline<*> -> "離線模式，顯示本機資料"
        is UiState.Unavailable -> "無法提供：${state.reason}"
        is UiState.Error -> "更新失敗：${state.message}"
    }
    Text(label, modifier = modifier.semantics { contentDescription = label })
}
