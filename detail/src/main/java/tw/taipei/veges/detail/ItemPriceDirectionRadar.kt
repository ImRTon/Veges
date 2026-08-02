package tw.taipei.veges.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.math.RoundingMode
import tw.taipei.veges.domain.ItemPriceDirection
import tw.taipei.veges.domain.ItemPriceDirectionEvaluation
import tw.taipei.veges.domain.ItemPriceDirectionStatus
import tw.taipei.veges.domain.MarketBasis

private val RisingRed = Color(0xFFE5484D)
private val FallingGreen = Color(0xFF00A86B)

@Composable
fun ItemPriceDirectionRadarCard(
    householdName: String,
    basis: MarketBasis,
    evaluation: ItemPriceDirectionEvaluation?,
    modifier: Modifier = Modifier,
) {
    val presentation = evaluation.toRadarPresentation()
    val accent = when (evaluation?.outlook?.direction) {
        ItemPriceDirection.RISING -> RisingRed
        ItemPriceDirection.FALLING -> FallingGreen
        null -> MaterialTheme.colorScheme.primary
    }
    val shape = RoundedCornerShape(28.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = radarAccessibilityText(
                    householdName = householdName,
                    basis = basis,
                    evaluation = evaluation,
                )
            },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
        tonalElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.16f),
                        accent.copy(alpha = 0.035f),
                        Color.Transparent,
                    ),
                ),
                shape = shape,
            ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "個別價格雷達",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            basis.radarLabel(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = accent.copy(alpha = 0.14f),
                    ) {
                        Text(
                            "預測 7–14 日",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(58.dp),
                        shape = CircleShape,
                        color = accent.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                presentation.symbol,
                                style = MaterialTheme.typography.headlineMedium,
                                color = accent,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            presentation.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (evaluation?.outlook == null) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                accent
                            },
                        )
                        Text(
                            presentation.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    evaluation?.outlook?.let { outlook ->
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                outlook.projectedChangePercent.signedPercent(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = accent,
                            )
                            Text(
                                "預估變動",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                evaluation?.outlook?.let { outlook ->
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "訊號強度",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "${outlook.strengthScore}/100",
                                style = MaterialTheme.typography.labelMedium,
                                color = accent,
                                fontWeight = FontWeight.Black,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { outlook.strengthScore / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = accent,
                            trackColor = accent.copy(alpha = 0.14f),
                        )
                    }
                    val secondaryReasons = outlook.reasons.drop(1).take(2)
                    if (secondaryReasons.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            secondaryReasons.forEach { reason ->
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = accent.copy(alpha = 0.11f),
                                    border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
                                ) {
                                    Text(
                                        reason.shortLabel,
                                        modifier = Modifier.padding(
                                            horizontal = 11.dp,
                                            vertical = 6.dp,
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = accent,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }

                evaluation?.let {
                    Text(
                        buildString {
                            append("分析 ${it.validTradingDayCount} 個有效交易日")
                            it.latestObservationDate?.let { date ->
                                append(" · 最新 ")
                                append(date)
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

            }
        }
    }
}

private data class RadarPresentation(
    val symbol: String,
    val title: String,
    val body: String,
)

private fun ItemPriceDirectionEvaluation?.toRadarPresentation(): RadarPresentation = when {
    this == null -> RadarPresentation(
        symbol = "…",
        title = "分析準備中",
        body = "正在整理這個品項的近期批發行情。",
    )

    status == ItemPriceDirectionStatus.INSUFFICIENT_HISTORY -> RadarPresentation(
        symbol = "…",
        title = "資料累積中",
        body = "已有 $validTradingDayCount/10 個有效交易日，累積完成後即可判讀。",
    )

    status == ItemPriceDirectionStatus.STALE_DATA -> RadarPresentation(
        symbol = "!",
        title = "行情需要更新",
        body = "最新資料已超過 4 日，暫不提供方向推估。",
    )

    status == ItemPriceDirectionStatus.NO_CLEAR_SIGNAL -> RadarPresentation(
        symbol = "—",
        title = "目前方向不明顯",
        body = "漲跌證據尚未形成一致訊號，先持續觀察近期價量。",
    )

    outlook?.direction == ItemPriceDirection.RISING -> RadarPresentation(
        symbol = "↗",
        title = "漲價訊號偏強",
        body = requireNotNull(outlook).reasons.firstOrNull()?.headline ?: "近期價量偏向上行。",
    )

    else -> RadarPresentation(
        symbol = "↘",
        title = "降價訊號偏強",
        body = outlook?.reasons?.firstOrNull()?.headline ?: "近期價量偏向下行。",
    )
}

private fun radarAccessibilityText(
    householdName: String,
    basis: MarketBasis,
    evaluation: ItemPriceDirectionEvaluation?,
): String {
    val presentation = evaluation.toRadarPresentation()
    val signal = evaluation?.outlook?.let { outlook ->
        val direction = if (outlook.direction == ItemPriceDirection.RISING) "上漲" else "下跌"
        val reasons = outlook.reasons.take(3).joinToString("、") { it.shortLabel }
        "$direction，預估 ${outlook.projectedChangePercent.signedPercent()}，" +
            "訊號強度 ${outlook.strengthScore} 分，原因 $reasons"
    } ?: presentation.body
    return "$householdName 個別價格雷達，${basis.radarLabel()}，預測 7 至 14 日。" +
        "${presentation.title}，$signal。"
}

private fun MarketBasis.radarLabel(): String = when (this) {
    MarketBasis.TAIPEI_COMBINED -> "台北合併"
    MarketBasis.TAIPEI_FIRST -> "台北一"
    MarketBasis.TAIPEI_SECOND -> "台北二"
}

private fun java.math.BigDecimal.signedPercent(): String {
    val rounded = setScale(1, RoundingMode.HALF_UP).toPlainString()
    return if (signum() > 0) "+$rounded%" else "$rounded%"
}
