package tw.taipei.veges.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tw.taipei.veges.domain.EstimateDisclosure
import tw.taipei.veges.domain.ProduceConceptId
import tw.taipei.veges.domain.TrendPeriod
import tw.taipei.veges.domain.TrendSummary
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.designsystem.EstimateDisclosureLabel
import tw.taipei.veges.designsystem.AiIllustrationDisclosure

@Composable
fun DetailRoute(
    conceptId: String,
    modifier: Modifier = Modifier,
) {
    val viewModel: DetailViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(conceptId) {
        viewModel.load(ProduceConceptId(conceptId))
    }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(state.concept?.householdName ?: "蔬果詳情準備中")
        Text("官方身份：${state.concept?.officialVariants?.joinToString { "${it.officialName} (${it.code.value})" } ?: "載入中"}")
        AiIllustrationDisclosure()
        HorizontalDivider()
        Text("估價資料基礎")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MarketBasis.entries.forEach { basis ->
                FilterChip(
                    selected = basis == state.selectedBasis,
                    onClick = { viewModel.selectBasis(basis) },
                    label = { Text(basis.displayName()) },
                )
            }
        }
        state.estimate?.let { estimate ->
            EstimateDisclosureLabel(disclosure = estimate.disclosure)
            Text(estimate.point?.let { "${it.amount} 元 / 台斤" } ?: "估價尚不可用")
            Text("批發來源日期：${estimate.wholesaleSourceDates.joinToString()}")
            Text("零售校準截止：${estimate.calibrationCutoff}")
        } ?: Text("此資料基礎目前無法提供估價：資料尚未通過校準門檻")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::toggleTracked) {
                Text(if (state.isTracked) "取消追蹤" else "追蹤")
            }
            OutlinedButton(onClick = { }) { Text("設定提醒") }
        }
        OutlinedButton(onClick = viewModel::toggleMethodology) { Text("方法與來源") }
        if (state.methodologyExpanded) {
            Text(
                "此頁估價為模型輸出，不是店家或市場觀測價格。" +
                    "頁面會顯示市場基礎、官方作物代碼、批發來源日期、零售校準截止日期與模型限制。",
            )
        }
        WholesaleTrendChart(
            summary = TrendSummary(
                period = TrendPeriod.SEVEN_DAYS,
                latest = null,
                minimumAverage = null,
                maximumAverage = null,
                directionText = "尚無足夠資料",
            ),
        )
    }
}

private fun MarketBasis.displayName(): String = when (this) {
    MarketBasis.TAIPEI_COMBINED -> "台北合併"
    MarketBasis.TAIPEI_FIRST -> "台北一"
    MarketBasis.TAIPEI_SECOND -> "台北二"
}

// TODO(7.4): Add qualified estimates, source controls, provenance, and actions.
// TODO(7.5): Add accessible trend visualizations and textual summaries.
