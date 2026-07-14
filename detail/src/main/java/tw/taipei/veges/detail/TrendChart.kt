package tw.taipei.veges.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import tw.taipei.veges.domain.TrendSummary
import tw.taipei.veges.domain.accessibilityText

@Composable
fun WholesaleTrendChart(
    summary: TrendSummary,
    modifier: Modifier = Modifier,
) {
    // TODO(7.5): Replace this textual architecture shell with an accessible range/average/volume chart.
    // The visual must not use OHLC/candlestick semantics because wholesale data has no open/close values.
    Column(
        modifier = modifier
            .padding(vertical = 12.dp)
            .semantics { contentDescription = summary.accessibilityText() },
    ) {
        Text("批發價格範圍與平均")
        Text(summary.accessibilityText())
    }
}
