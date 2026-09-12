package tw.taipei.veges.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.math.RoundingMode
import tw.taipei.veges.designsystem.PillChoiceRow
import tw.taipei.veges.domain.TrendPeriod
import tw.taipei.veges.domain.TrendPoint
import tw.taipei.veges.domain.TrendSummary
import tw.taipei.veges.domain.accessibilityText
import tw.taipei.veges.domain.Estimate
import tw.taipei.veges.domain.CandleDirection
import tw.taipei.veges.domain.EstimationMath
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.toProduceMarketCandles

@Composable
fun WholesaleTrendChart(
    points: List<TrendPoint>,
    estimateHistory: List<Estimate> = emptyList(),
    period: TrendPeriod,
    onPeriodSelected: (TrendPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val validPoints = points.filter { it.averageNtdPerKg != null }.sortedBy { it.date }
    val candles = validPoints.toProduceMarketCandles()
    val averages = validPoints.map { requireNotNull(it.averageNtdPerKg) }
    val latest = validPoints.lastOrNull()
    val summary = TrendSummary(
        period = period,
        latest = latest,
        minimumAverage = averages.minOrNull(),
        maximumAverage = averages.maxOrNull(),
        directionText = directionText(averages),
    )
    var selectedIndex by remember(candles) {
        mutableIntStateOf((candles.lastIndex).coerceAtLeast(0))
    }
    val selected = candles.getOrNull(selectedIndex)
    Surface(
        modifier = modifier
            .padding(vertical = 8.dp)
            .semantics { contentDescription = summary.accessibilityText() },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("價格歷史", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "所有價格：元 / 台斤",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    latest?.averageNtdPerKg
                        ?.let(EstimationMath::ntdPerKilogramToNtdPerTaiJin)
                        ?.setScale(1, RoundingMode.HALF_UP)
                        ?.toPlainString()
                        ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
            }
            PillChoiceRow(
                items = TrendPeriod.entries,
                selectedItem = period,
                onItemSelected = onPeriodSelected,
                itemLabel = TrendPeriod::shortLabel,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedContentColor = MaterialTheme.colorScheme.onPrimary,
            )
            Text("蔬果市場區間 K 線", style = MaterialTheme.typography.titleMedium)
            if (candles.isNotEmpty()) {
                val priceValues = candles.flatMap { candle ->
                    listOfNotNull(
                        candle.previousAverageNtdPerKg,
                        candle.currentAverageNtdPerKg,
                        candle.officialLowNtdPerKg,
                        candle.officialHighNtdPerKg,
                    )
                }
                val minimum = priceValues.minOrNull() ?: BigDecimal.ZERO
                val maximum = priceValues.maxOrNull() ?: BigDecimal.ONE
                val maximumVolume = candles.mapNotNull { it.volumeKg }.maxOrNull()
                    ?.takeIf { it.signum() > 0 }
                val grid = MaterialTheme.colorScheme.outlineVariant
                val marker = MaterialTheme.colorScheme.onSurface
                val rising = Color(0xFFFF5D68)
                val falling = Color(0xFF16A477)
                val unchanged = MaterialTheme.colorScheme.onSurfaceVariant
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(246.dp)
                        .pointerInput(candles) {
                            detectTapGestures { offset ->
                                val step = size.width.toFloat() / candles.size.coerceAtLeast(1)
                                selectedIndex = (offset.x / step)
                                    .toInt()
                                    .coerceIn(0, candles.lastIndex)
                            }
                        }
                        .pointerInput(candles) {
                            fun selectAt(x: Float) {
                                val step = size.width.toFloat() / candles.size.coerceAtLeast(1)
                                selectedIndex = (x / step)
                                    .toInt()
                                    .coerceIn(0, candles.lastIndex)
                            }
                            detectHorizontalDragGestures(
                                onDragStart = { selectAt(it.x) },
                            ) { change, _ ->
                                selectAt(change.position.x)
                                change.consume()
                            }
                        },
                ) {
                    val priceTop = 10.dp.toPx()
                    val priceBottom = 172.dp.toPx()
                    val volumeTop = 194.dp.toPx()
                    val volumeBottom = size.height - 8.dp.toPx()
                    repeat(4) { line ->
                        val y = priceTop + (priceBottom - priceTop) * line / 3f
                        drawLine(grid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                    }
                    val spread = maximum.subtract(minimum)
                    fun priceY(value: BigDecimal): Float {
                        val ratio = if (spread.signum() == 0) {
                            0.5f
                        } else {
                            value
                                .subtract(minimum)
                                .divide(spread, 6, RoundingMode.HALF_UP)
                                .toFloat()
                                .coerceIn(0f, 1f)
                        }
                        return priceBottom - ratio * (priceBottom - priceTop)
                    }
                    val slotWidth = size.width / candles.size.coerceAtLeast(1)
                    val bodyWidth = (slotWidth * 0.52f).coerceIn(5.dp.toPx(), 18.dp.toPx())
                    candles.forEachIndexed { index, candle ->
                        val x = slotWidth * (index + 0.5f)
                        val candleColor = when (candle.direction) {
                            CandleDirection.RISING -> rising
                            CandleDirection.FALLING -> falling
                            CandleDirection.UNCHANGED -> unchanged
                        }
                        val officialLow = candle.officialLowNtdPerKg
                        val officialHigh = candle.officialHighNtdPerKg
                        if (officialLow != null && officialHigh != null) {
                            drawLine(
                                candleColor,
                                Offset(x, priceY(officialHigh)),
                                Offset(x, priceY(officialLow)),
                                1.5.dp.toPx(),
                            )
                        }
                        val previousY = priceY(candle.previousAverageNtdPerKg)
                        val currentY = priceY(candle.currentAverageNtdPerKg)
                        val rawTop = minOf(previousY, currentY)
                        val bodyHeight = kotlin.math.abs(previousY - currentY).coerceAtLeast(2.dp.toPx())
                        val bodyTop = if (bodyHeight > kotlin.math.abs(previousY - currentY)) {
                            rawTop - bodyHeight / 2f
                        } else {
                            rawTop
                        }
                        drawRect(
                            color = candleColor,
                            topLeft = Offset(x - bodyWidth / 2f, bodyTop),
                            size = Size(bodyWidth, bodyHeight),
                        )
                        candle.volumeKg?.takeIf { it.signum() >= 0 }?.let { volume ->
                            val volumeRatio = if (maximumVolume == null) {
                                0f
                            } else {
                                volume.divide(maximumVolume, 6, RoundingMode.HALF_UP)
                                    .toFloat()
                                    .coerceIn(0f, 1f)
                            }
                            val barHeight = (volumeBottom - volumeTop) * volumeRatio
                            drawRect(
                                color = candleColor.copy(alpha = 0.72f),
                                topLeft = Offset(x - bodyWidth / 2f, volumeBottom - barHeight),
                                size = Size(bodyWidth, barHeight),
                            )
                        }
                        if (index == selectedIndex) {
                            drawLine(
                                color = marker.copy(alpha = 0.42f),
                                start = Offset(x, priceTop),
                                end = Offset(x, volumeBottom),
                                strokeWidth = 1.dp.toPx(),
                            )
                            drawCircle(
                                color = marker,
                                radius = bodyWidth / 2f + 3.dp.toPx(),
                                center = Offset(x, currentY),
                                style = Stroke(width = 1.5.dp.toPx()),
                            )
                        }
                    }
                }
                selected?.let { candle ->
                    Text(
                        "${candle.date}　${candle.currentAverageNtdPerKg.price()} / 台斤　" +
                            candle.direction.displayText(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "高 ${candle.officialHighNtdPerKg.priceOrDash()}　" +
                            "低 ${candle.officialLowNtdPerKg.priceOrDash()}　" +
                            "量 ${candle.volumeKg?.setScale(0, RoundingMode.HALF_UP) ?: "—"} kg",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    "尚無足夠交易日資料",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(summary.directionText, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun WholesaleTrendChart(
    summary: TrendSummary,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .padding(vertical = 12.dp)
            .semantics { contentDescription = summary.accessibilityText() },
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("蔬果市場區間 K 線", style = MaterialTheme.typography.titleMedium)
            Text(
                "${summary.period.days}日 · ${summary.directionText}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun directionText(averages: List<BigDecimal>): String {
    if (averages.size < 2) return "尚無足夠資料"
    val first = averages.first()
    val latest = averages.last()
    return when {
        latest > first -> "期間平均價走高"
        latest < first -> "期間平均價走低"
        else -> "期間平均價持平"
    }
}

private fun TrendPeriod.shortLabel(): String = when (this) {
    TrendPeriod.SEVEN_DAYS -> "7日"
    TrendPeriod.THIRTY_DAYS -> "30日"
    TrendPeriod.NINETY_DAYS -> "90日"
    TrendPeriod.ONE_YEAR -> "1年"
}

private fun CandleDirection.displayText(): String = when (this) {
    CandleDirection.RISING -> "▲ 上漲"
    CandleDirection.FALLING -> "▼ 下跌"
    CandleDirection.UNCHANGED -> "— 持平"
}

private fun MarketBasis.displayText(): String = when (this) {
    MarketBasis.TAIPEI_COMBINED -> "台北合併"
    MarketBasis.TAIPEI_FIRST -> "台北一"
    MarketBasis.TAIPEI_SECOND -> "台北二"
}

private fun BigDecimal.price(): String =
    EstimationMath.ntdPerKilogramToNtdPerTaiJin(this)
        .setScale(1, RoundingMode.HALF_UP)
        .toPlainString()

private fun BigDecimal?.priceOrDash(): String =
    this?.price() ?: "—"
