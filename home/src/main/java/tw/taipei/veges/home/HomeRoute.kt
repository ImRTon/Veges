package tw.taipei.veges.home

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tw.taipei.veges.designsystem.ProduceIllustration
import tw.taipei.veges.domain.HomeItem
import tw.taipei.veges.domain.MarketItem
import tw.taipei.veges.domain.MarketPriceSurgeOutlook
import tw.taipei.veges.domain.PriceSurgeReason
import tw.taipei.veges.domain.PriceSurgeReasonKind
import tw.taipei.veges.domain.PriceSurgeRiskLevel
import tw.taipei.veges.domain.PriceRefresh
import tw.taipei.veges.domain.PriceRefreshStage
import tw.taipei.veges.domain.averageChangePercent
import tw.taipei.veges.domain.previousChangePercent

private val RisingRed = Color(0xFFE5484D)
private val FallingGreen = Color(0xFF00A86B)
private val HomeTabs = listOf("追蹤", "大跌")
private val AnimationTestReasons = listOf(
    PriceSurgeReason(
        kind = PriceSurgeReasonKind.TYPHOON,
        contribution = 0,
        headline = "颱風來襲，整體蔬果價格可能上揚",
        shortLabel = "颱風",
    ),
    PriceSurgeReason(
        kind = PriceSurgeReasonKind.HEAVY_RAIN,
        contribution = 0,
        headline = "連續暴雨，整體蔬果價格可能上揚",
        shortLabel = "暴雨",
    ),
    PriceSurgeReason(
        kind = PriceSurgeReasonKind.EXTREME_HEAT,
        contribution = 0,
        headline = "高溫持續，整體蔬果供應承壓",
        shortLabel = "高溫",
    ),
    PriceSurgeReason(
        kind = PriceSurgeReasonKind.VOLUME_CONTRACTION,
        contribution = 0,
        headline = "到貨量普遍縮減，整體價格可能上揚",
        shortLabel = "到貨量縮",
    ),
    PriceSurgeReason(
        kind = PriceSurgeReasonKind.PRICE_MOMENTUM,
        contribution = 0,
        headline = "多項蔬果價格同步上揚",
        shortLabel = "價格動能",
    ),
    PriceSurgeReason(
        kind = PriceSurgeReasonKind.RECENT_PRICE_ANOMALY,
        contribution = 0,
        headline = "多項蔬果價格高於近期常態",
        shortLabel = "價格異常",
    ),
)

// Material 3 1.4 keeps MotionScheme internal. These are its default spatial spring tokens.
private val Material3DefaultSpatialSpec: AnimationSpec<Float> = spring(
    dampingRatio = 0.9f,
    stiffness = 700f,
)

@Composable
fun HomeRoute(
    onBrowseCatalog: () -> Unit,
    onConceptSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onBrowseCatalog = onBrowseCatalog,
        onRefresh = viewModel::refresh,
        onDeclinerLookbackSelected = viewModel::setDeclinerLookbackDays,
        onConceptSelected = onConceptSelected,
        modifier = modifier,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    state: HomeUiState,
    onBrowseCatalog: () -> Unit,
    onRefresh: () -> Unit,
    onDeclinerLookbackSelected: (Int) -> Unit,
    onConceptSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = HomeTabs::size)
    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = Material3DefaultSpatialSpec,
    )
    val trackedListState = rememberLazyListState()
    val declinersListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var radarExpandedHeightPx by remember { mutableFloatStateOf(0f) }
    var radarCollapsePx by remember { mutableFloatStateOf(0f) }
    val radarNestedScrollConnection = remember(coroutineScope) {
        object : NestedScrollConnection {
            private var settleJob: Job? = null

            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (available.y == 0f || radarExpandedHeightPx <= 0f) return Offset.Zero
                if (available.y < 0f && radarCollapsePx >= radarExpandedHeightPx) {
                    return Offset.Zero
                }
                if (available.y > 0f && radarCollapsePx <= 0f) return Offset.Zero
                settleJob?.cancel()
                val previous = radarCollapsePx
                radarCollapsePx = (previous - available.y)
                    .coerceIn(0f, radarExpandedHeightPx)
                return Offset(x = 0f, y = previous - radarCollapsePx)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (radarExpandedHeightPx <= 0f) return Velocity.Zero
                val target = when {
                    available.y < 0f -> radarExpandedHeightPx
                    available.y > 0f -> 0f
                    radarCollapsePx >= radarExpandedHeightPx / 2f -> radarExpandedHeightPx
                    else -> 0f
                }
                settleJob?.cancel()
                settleJob = coroutineScope.launch {
                    animate(
                        initialValue = radarCollapsePx,
                        targetValue = target,
                        animationSpec = Material3DefaultSpatialSpec,
                    ) { value, _ ->
                        radarCollapsePx = value.coerceIn(0f, radarExpandedHeightPx)
                    }
                }
                return Velocity.Zero
            }
        }
    }
    Column(
        modifier
            .fillMaxSize()
            .nestedScroll(radarNestedScrollConnection),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 18.dp, end = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "行情",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "更新行情")
            }
            IconButton(onClick = onBrowseCatalog) {
                Icon(Icons.Rounded.Add, contentDescription = "新增追蹤")
            }
        }
        PriceRefreshProgress(state.priceRefresh)
        PriceSurgeRadarSection(
            outlook = state.marketPriceSurgeOutlook,
            eligibleItemCount = state.predictionEligibleCount,
            modifier = Modifier
                .clipToBounds()
                .collapseFromTop { radarCollapsePx }
                .onSizeChanged { measuredSize ->
                    val measuredHeight = measuredSize.height.toFloat()
                    if (measuredHeight != radarExpandedHeightPx) {
                        radarExpandedHeightPx = measuredHeight
                        radarCollapsePx = radarCollapsePx.coerceAtMost(measuredHeight)
                    }
                },
        )
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            indicator = {
                Canvas(
                    modifier = Modifier
                        .tabIndicatorOffset(pagerState.currentPage)
                        .height(48.dp),
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.width / 2f
                    withTransform({
                        scale(
                            scaleX = 1f,
                            scaleY = size.height / size.width,
                            pivot = center,
                        )
                    }) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                0f to FallingGreen.copy(alpha = 0.18f),
                                0.52f to FallingGreen.copy(alpha = 0.11f),
                                0.82f to FallingGreen.copy(alpha = 0.035f),
                                1f to Color.Transparent,
                                center = center,
                                radius = radius,
                            ),
                            radius = radius,
                            center = center,
                        )
                    }
                }
            },
        ) {
            CompositionLocalProvider(LocalRippleConfiguration provides null) {
                HomeTabs.forEachIndexed { index, label ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    page = index,
                                    animationSpec = Material3DefaultSpatialSpec,
                                )
                            }
                        },
                        text = {
                            Text(
                                label,
                                fontWeight = if (pagerState.currentPage == index) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Medium
                                },
                            )
                        },
                    )
                }
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            flingBehavior = pagerFlingBehavior,
            key = HomeTabs::get,
        ) { page ->
            when (page) {
                0 -> TrackedList(
                    items = state.tracked,
                    listState = trackedListState,
                    onBrowseCatalog = onBrowseCatalog,
                    onConceptSelected = onConceptSelected,
                )

                else -> DeclinersList(
                    items = state.decliners,
                    eligibleCount = state.declinerEligibleCount,
                    listState = declinersListState,
                    lookbackDays = state.declinerLookbackDays,
                    onLookbackSelected = onDeclinerLookbackSelected,
                    onConceptSelected = onConceptSelected,
                )
            }
        }
    }
}

@Composable
private fun PriceRefreshProgress(refresh: PriceRefresh) {
    if (!refresh.isRunning) return
    val label = when (refresh.stage) {
        PriceRefreshStage.PREPARING -> "準備更新行情"
        PriceRefreshStage.LATEST_PRICES -> "正在下載最新行情"
        PriceRefreshStage.HISTORY -> "正在下載歷史行情"
        PriceRefreshStage.SAVING -> "正在整理價格資料"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .semantics {
                contentDescription = label
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PriceSurgeRadarSection(
    outlook: MarketPriceSurgeOutlook?,
    eligibleItemCount: Int,
    modifier: Modifier = Modifier,
) {
    var animationTestMode by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    "漲價雷達",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "預測 7–14 日",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Surface(
                onClick = { animationTestMode = !animationTestMode },
                modifier = Modifier.semantics {
                    contentDescription = if (animationTestMode) {
                        "結束動畫測試"
                    } else {
                        "測試所有漲價原因動畫"
                    }
                },
                shape = RoundedCornerShape(12.dp),
                color = if (animationTestMode) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = if (animationTestMode) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
            ) {
                Text(
                    if (animationTestMode) "結束測試" else "測試動畫",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (animationTestMode) {
            AnimationTestPanel()
        } else if (outlook == null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .semantics {
                        contentDescription = if (eligibleItemCount == 0) {
                            "漲價雷達，資料累積中"
                        } else {
                            "漲價雷達，目前沒有大範圍漲價訊號"
                        }
                    },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AnimatedRiskIcon(
                        reason = PriceSurgeReason(
                            kind = PriceSurgeReasonKind.RECENT_PRICE_ANOMALY,
                            contribution = 0,
                            headline = "行情掃描中",
                            shortLabel = "行情掃描",
                        ),
                        modifier = Modifier.size(42.dp),
                    )
                    Column {
                        Text(
                            if (eligibleItemCount == 0) {
                                "正在累積預測所需行情"
                            } else {
                                "目前沒有大範圍漲價訊號"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            if (eligibleItemCount == 0) {
                                "至少需要 10 個有效交易日"
                            } else {
                                "已分析 $eligibleItemCount 項蔬果 · 模型預測不是保證"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            MarketPriceSurgeOutlookCard(outlook)
            Text(
                "至少 10 項具足夠歷史，且 20% 以上同步承壓才顯示 · 模型預測不是保證",
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun Modifier.collapseFromTop(
    offsetPx: () -> Float,
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val offset = offsetPx().roundToInt().coerceIn(0, placeable.height)
    layout(
        width = placeable.width,
        height = placeable.height - offset,
    ) {
        placeable.placeRelative(x = 0, y = -offset)
    }
}

@Composable
private fun AnimationTestPanel() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .semantics {
                contentDescription = "動畫測試模式，六種漲價原因動畫正在播放"
            },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "動態訊號圖鑑",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "即時預覽",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            AnimationTestReasons.chunked(3).forEach { reasons ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    reasons.forEach { reason ->
                        AnimationTestReason(
                            reason = reason,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimationTestReason(
    reason: PriceSurgeReason,
    modifier: Modifier = Modifier,
) {
    val accent = reason.kind.animationAccent()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AnimatedRiskIcon(
            reason = reason,
            modifier = Modifier.size(72.dp),
            accent = accent,
        )
        Text(
            reason.shortLabel,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PriceSurgeReasonKind.animationAccent(): Color = when (this) {
    PriceSurgeReasonKind.TYPHOON -> Color(0xFF7567D8)
    PriceSurgeReasonKind.HEAVY_RAIN -> Color(0xFF3478F6)
    PriceSurgeReasonKind.EXTREME_HEAT -> Color(0xFFE87800)
    PriceSurgeReasonKind.VOLUME_CONTRACTION -> FallingGreen
    PriceSurgeReasonKind.PRICE_MOMENTUM -> RisingRed
    PriceSurgeReasonKind.RECENT_PRICE_ANOMALY -> MaterialTheme.colorScheme.primary
}

@Composable
private fun MarketPriceSurgeOutlookCard(
    outlook: MarketPriceSurgeOutlook,
) {
    val accent = when (outlook.riskLevel) {
        PriceSurgeRiskLevel.HIGH -> RisingRed
        PriceSurgeRiskLevel.ELEVATED -> Color(0xFFE87800)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .heightIn(min = 126.dp)
            .semantics {
                contentDescription =
                    "${outlook.primaryReason.headline}，" +
                        "${outlook.affectedItemCount}項蔬果同步出現訊號，" +
                        "市場廣度${outlook.marketBreadthPercent}%，" +
                        "整體風險分數${outlook.riskScore}"
            },
        shape = RoundedCornerShape(22.dp),
        color = accent.copy(alpha = 0.09f),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            AnimatedRiskIcon(
                reason = outlook.primaryReason,
                modifier = Modifier.size(width = 84.dp, height = 118.dp),
                accent = outlook.primaryReason.kind.animationAccent(),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    outlook.primaryReason.headline,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                Text(
                    "${outlook.affectedItemCount}/${outlook.eligibleItemCount} 項同步承壓" +
                        " · 市場廣度 ${outlook.marketBreadthPercent}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${outlook.horizonStartDays}–${outlook.horizonEndDays} 日" +
                        " · 受影響品項平均 +${outlook.projectedRisePercent}%" +
                        " · 風險 ${outlook.riskScore} 分",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (outlook.affectedNames.isNotEmpty()) {
                    Text(
                        "較明顯：${outlook.affectedNames.joinToString("、")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedRiskIcon(
    reason: PriceSurgeReason,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val transition = rememberInfiniteTransition(label = "risk-${reason.kind}")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (reason.kind) {
                    PriceSurgeReasonKind.TYPHOON -> 4_200
                    PriceSurgeReasonKind.HEAVY_RAIN -> 1_900
                    PriceSurgeReasonKind.EXTREME_HEAT -> 3_200
                    PriceSurgeReasonKind.VOLUME_CONTRACTION -> 2_600
                    PriceSurgeReasonKind.PRICE_MOMENTUM -> 2_800
                    PriceSurgeReasonKind.RECENT_PRICE_ANOMALY -> 3_200
                },
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "risk-progress",
    )
    Surface(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = reason.headline
        },
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.105f),
        contentColor = accent,
        border = BorderStroke(
            width = 1.dp,
            color = accent.copy(alpha = 0.16f),
        ),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(7.dp),
        ) {
            val unit = size.minDimension
            val center = Offset(size.width / 2f, size.height / 2f)
            val tau = (PI * 2).toFloat()
            drawCircle(
                color = accent.copy(alpha = 0.045f),
                radius = unit * 0.48f,
                center = center,
            )
            when (reason.kind) {
                PriceSurgeReasonKind.HEAVY_RAIN -> {
                    val cloudShift = sin(progress * tau) * unit * 0.018f
                    val cloudY = size.height * 0.3f
                    drawCircle(
                        color = accent.copy(alpha = 0.66f),
                        radius = unit * 0.145f,
                        center = Offset(size.width * 0.37f + cloudShift, cloudY + unit * 0.015f),
                    )
                    drawCircle(
                        color = accent.copy(alpha = 0.88f),
                        radius = unit * 0.195f,
                        center = Offset(size.width * 0.54f + cloudShift, cloudY - unit * 0.045f),
                    )
                    drawCircle(
                        color = accent.copy(alpha = 0.72f),
                        radius = unit * 0.13f,
                        center = Offset(size.width * 0.7f + cloudShift, cloudY + unit * 0.025f),
                    )
                    drawRoundRect(
                        color = accent.copy(alpha = 0.78f),
                        topLeft = Offset(size.width * 0.22f + cloudShift, cloudY),
                        size = Size(size.width * 0.6f, unit * 0.17f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            unit * 0.085f,
                            unit * 0.085f,
                        ),
                    )
                    val groundY = size.height * 0.84f
                    drawLine(
                        color = accent.copy(alpha = 0.16f),
                        start = Offset(size.width * 0.15f, groundY),
                        end = Offset(size.width * 0.85f, groundY),
                        strokeWidth = unit * 0.018f,
                        cap = StrokeCap.Round,
                    )
                    repeat(6) { index ->
                        val dropProgress = (progress + index * 0.165f) % 1f
                        val x = size.width * (0.19f + index * 0.125f)
                        val startY = size.height * 0.47f
                        val y = startY + (groundY - startY) * dropProgress
                        val alpha = sin(dropProgress * PI).toFloat().coerceAtLeast(0f)
                        drawLine(
                            color = accent.copy(alpha = 0.25f + alpha * 0.72f),
                            start = Offset(x + unit * 0.055f, y - unit * 0.12f),
                            end = Offset(x, y),
                            strokeWidth = unit * 0.03f,
                            cap = StrokeCap.Round,
                        )
                    }
                    repeat(3) { index ->
                        val ripple = (progress + index * 0.34f) % 1f
                        val rippleAlpha = (1f - ripple) * 0.42f
                        val rippleWidth = unit * (0.07f + ripple * 0.18f)
                        drawOval(
                            color = accent.copy(alpha = rippleAlpha),
                            topLeft = Offset(
                                size.width * (0.3f + index * 0.2f) - rippleWidth / 2f,
                                groundY - unit * 0.018f,
                            ),
                            size = Size(rippleWidth, unit * 0.045f),
                            style = Stroke(width = unit * 0.016f),
                        )
                    }
                }

                PriceSurgeReasonKind.TYPHOON -> {
                    val spin = -progress * 360f
                    repeat(5) { layer ->
                        val radius = unit * (0.13f + layer * 0.072f)
                        drawArc(
                            color = accent.copy(alpha = 0.92f - layer * 0.13f),
                            startAngle = spin + layer * 74f,
                            sweepAngle = 105f + layer * 3f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2f, radius * 2f),
                            style = Stroke(
                                width = unit * (0.064f - layer * 0.007f),
                                cap = StrokeCap.Round,
                            ),
                        )
                    }
                    repeat(9) { index ->
                        val particleProgress = (progress + index / 8f) % 1f
                        val angle = -progress * PI * 2 * 0.9 +
                            index * PI * 2 / 9 -
                            particleProgress * 1.35
                        val radius = unit * (0.43f - particleProgress * 0.27f)
                        val alpha = sin(particleProgress * PI).toFloat().coerceAtLeast(0f)
                        drawCircle(
                            color = accent.copy(alpha = alpha * 0.55f),
                            radius = unit * (0.012f + particleProgress * 0.012f),
                            center = Offset(
                                center.x + cos(angle).toFloat() * radius,
                                center.y + sin(angle).toFloat() * radius,
                            ),
                        )
                    }
                    drawCircle(
                        color = accent.copy(alpha = 0.18f),
                        radius = unit * 0.105f,
                        center = center,
                    )
                    drawCircle(
                        color = accent.copy(alpha = 0.95f),
                        radius = unit * 0.04f,
                        center = center,
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.7f),
                        radius = unit * 0.014f,
                        center = Offset(center.x - unit * 0.012f, center.y - unit * 0.012f),
                    )
                }

                PriceSurgeReasonKind.EXTREME_HEAT -> {
                    val sunCenter = Offset(size.width * 0.5f, size.height * 0.33f)
                    val pulse = 1f + sin(progress * tau) * 0.035f
                    val sunRadius = unit * 0.17f * pulse
                    repeat(12) { index ->
                        val angle = progress * PI * 2 * 0.12 + index * PI * 2 / 12
                        val rayPulse = (
                            sin(progress * tau + index * 0.7f) + 1f
                            ) / 2f
                        val inner = sunRadius * 1.32f
                        val outer = sunRadius * (1.55f + rayPulse * 0.12f)
                        drawLine(
                            color = accent.copy(alpha = 0.48f + rayPulse * 0.26f),
                            start = Offset(
                                sunCenter.x + cos(angle).toFloat() * inner,
                                sunCenter.y + sin(angle).toFloat() * inner,
                            ),
                            end = Offset(
                                sunCenter.x + cos(angle).toFloat() * outer,
                                sunCenter.y + sin(angle).toFloat() * outer,
                            ),
                            strokeWidth = unit * 0.025f,
                            cap = StrokeCap.Round,
                        )
                    }
                    drawCircle(
                        color = accent.copy(alpha = 0.88f),
                        radius = sunRadius,
                        center = sunCenter,
                    )
                    repeat(3) { index ->
                        val riseProgress = (progress + index * 0.32f) % 1f
                        val baseY = size.height * (0.92f - riseProgress * 0.28f)
                        val alpha = sin(riseProgress * PI).toFloat().coerceAtLeast(0f)
                        val path = Path()
                        repeat(8) { step ->
                            val fraction = step / 7f
                            val y = baseY - size.height * 0.14f * fraction
                            val x = size.width * (0.32f + index * 0.18f) +
                                sin(fraction * PI * 2.2f + index * 0.9f)
                                    .toFloat() * unit * 0.025f
                            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path = path,
                            color = accent.copy(alpha = alpha * 0.55f),
                            style = Stroke(width = unit * 0.023f, cap = StrokeCap.Round),
                        )
                    }
                }

                PriceSurgeReasonKind.VOLUME_CONTRACTION -> {
                    val squeeze = (1f - cos(progress * tau)) / 2f
                    val rowWidths = listOf(0.68f, 0.54f, 0.4f)
                    rowWidths.forEachIndexed { index, widthFraction ->
                        val width = size.width * widthFraction * (1f - squeeze * 0.24f)
                        val height = unit * 0.105f
                        val y = size.height * (0.31f + index * 0.19f)
                        drawRoundRect(
                            color = accent.copy(alpha = 0.84f - index * 0.14f),
                            topLeft = Offset(center.x - width / 2f, y),
                            size = Size(width, height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                height / 2f,
                                height / 2f,
                            ),
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.5f),
                            radius = unit * 0.018f,
                            center = Offset(center.x, y + height / 2f),
                        )
                    }
                    val arrowInset = size.width * (0.13f + squeeze * 0.13f)
                    val arrowY = size.height * 0.79f
                    val arrowHalf = unit * 0.07f
                    drawLine(
                        color = accent.copy(alpha = 0.8f),
                        start = Offset(arrowInset, arrowY),
                        end = Offset(center.x - unit * 0.11f, arrowY),
                        strokeWidth = unit * 0.025f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = accent.copy(alpha = 0.8f),
                        start = Offset(size.width - arrowInset, arrowY),
                        end = Offset(center.x + unit * 0.11f, arrowY),
                        strokeWidth = unit * 0.025f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = accent,
                        start = Offset(center.x - unit * 0.11f, arrowY),
                        end = Offset(center.x - unit * 0.11f - arrowHalf, arrowY - arrowHalf),
                        strokeWidth = unit * 0.028f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = accent,
                        start = Offset(center.x - unit * 0.11f, arrowY),
                        end = Offset(center.x - unit * 0.11f - arrowHalf, arrowY + arrowHalf),
                        strokeWidth = unit * 0.028f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = accent,
                        start = Offset(center.x + unit * 0.11f, arrowY),
                        end = Offset(center.x + unit * 0.11f + arrowHalf, arrowY - arrowHalf),
                        strokeWidth = unit * 0.028f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = accent,
                        start = Offset(center.x + unit * 0.11f, arrowY),
                        end = Offset(center.x + unit * 0.11f + arrowHalf, arrowY + arrowHalf),
                        strokeWidth = unit * 0.028f,
                        cap = StrokeCap.Round,
                    )
                }

                PriceSurgeReasonKind.PRICE_MOMENTUM -> {
                    val left = size.width * 0.14f
                    val right = size.width * 0.86f
                    val top = size.height * 0.17f
                    val bottom = size.height * 0.84f
                    drawLine(
                        color = accent.copy(alpha = 0.18f),
                        start = Offset(left, top),
                        end = Offset(left, bottom),
                        strokeWidth = unit * 0.018f,
                    )
                    drawLine(
                        color = accent.copy(alpha = 0.18f),
                        start = Offset(left, bottom),
                        end = Offset(right, bottom),
                        strokeWidth = unit * 0.018f,
                    )
                    val trendPath = Path()
                    repeat(25) { step ->
                        val fraction = step / 24f
                        val x = left + (right - left) * fraction
                        val eased = fraction * fraction * (3f - 2f * fraction)
                        val y = bottom - (bottom - top) * (0.08f + 0.76f * eased)
                        if (step == 0) trendPath.moveTo(x, y) else trendPath.lineTo(x, y)
                    }
                    drawPath(
                        path = trendPath,
                        color = accent.copy(alpha = 0.22f),
                        style = Stroke(width = unit * 0.028f, cap = StrokeCap.Round),
                    )
                    val movingX = left + (right - left) * progress
                    val movingEase = progress * progress * (3f - 2f * progress)
                    val movingY = bottom - (bottom - top) * (0.08f + 0.76f * movingEase)
                    repeat(4) { trail ->
                        val trailProgress = (progress - trail * 0.055f).coerceAtLeast(0f)
                        val trailEase =
                            trailProgress * trailProgress * (3f - 2f * trailProgress)
                        val trailX = left + (right - left) * trailProgress
                        val trailY = bottom -
                            (bottom - top) * (0.08f + 0.76f * trailEase)
                        val fade = sin(progress * PI).toFloat().coerceAtLeast(0f)
                        drawCircle(
                            color = accent.copy(alpha = fade * (0.5f - trail * 0.09f)),
                            radius = unit * (0.032f - trail * 0.004f),
                            center = Offset(trailX, trailY),
                        )
                    }
                    drawCircle(
                        color = accent.copy(
                            alpha = sin(progress * PI).toFloat().coerceAtLeast(0f) * 0.14f,
                        ),
                        radius = unit * 0.105f,
                        center = Offset(movingX, movingY),
                    )
                    drawCircle(
                        color = accent.copy(
                            alpha = sin(progress * PI).toFloat().coerceAtLeast(0f),
                        ),
                        radius = unit * 0.042f,
                        center = Offset(movingX, movingY),
                    )
                    val tip = Offset(
                        right,
                        bottom - (bottom - top) * 0.84f,
                    )
                    drawLine(
                        color = accent.copy(alpha = 0.75f),
                        start = tip,
                        end = Offset(tip.x - unit * 0.105f, tip.y + unit * 0.02f),
                        strokeWidth = unit * 0.034f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = accent.copy(alpha = 0.75f),
                        start = tip,
                        end = Offset(tip.x - unit * 0.025f, tip.y + unit * 0.1f),
                        strokeWidth = unit * 0.034f,
                        cap = StrokeCap.Round,
                    )
                }

                PriceSurgeReasonKind.RECENT_PRICE_ANOMALY -> {
                    val radarRadius = unit * 0.39f
                    repeat(3) { ring ->
                        drawCircle(
                            color = accent.copy(alpha = 0.15f + ring * 0.055f),
                            radius = radarRadius * (ring + 1) / 3f,
                            center = center,
                            style = Stroke(width = unit * 0.014f),
                        )
                    }
                    drawLine(
                        color = accent.copy(alpha = 0.18f),
                        start = Offset(center.x - radarRadius, center.y),
                        end = Offset(center.x + radarRadius, center.y),
                        strokeWidth = unit * 0.012f,
                    )
                    drawLine(
                        color = accent.copy(alpha = 0.18f),
                        start = Offset(center.x, center.y - radarRadius),
                        end = Offset(center.x, center.y + radarRadius),
                        strokeWidth = unit * 0.012f,
                    )
                    val sweepDegrees = progress * 360f - 90f
                    repeat(7) { trail ->
                        drawArc(
                            color = accent.copy(alpha = 0.018f + trail * 0.012f),
                            startAngle = sweepDegrees - 42f + trail * 6f,
                            sweepAngle = 6.5f,
                            useCenter = true,
                            topLeft = Offset(
                                center.x - radarRadius,
                                center.y - radarRadius,
                            ),
                            size = Size(radarRadius * 2f, radarRadius * 2f),
                        )
                    }
                    val sweepAngle = Math.toRadians(sweepDegrees.toDouble())
                    drawLine(
                        color = accent.copy(alpha = 0.92f),
                        start = center,
                        end = Offset(
                            center.x + cos(sweepAngle).toFloat() * radarRadius,
                            center.y + sin(sweepAngle).toFloat() * radarRadius,
                        ),
                        strokeWidth = unit * 0.022f,
                        cap = StrokeCap.Round,
                    )
                    val blips = listOf(
                        Offset(-0.2f, -0.13f),
                        Offset(0.23f, -0.19f),
                        Offset(0.15f, 0.24f),
                    )
                    blips.forEach { blip ->
                        val targetDegrees = Math.toDegrees(
                            atan2(blip.y.toDouble(), blip.x.toDouble()),
                        ).toFloat()
                        val normalizedSweep = (sweepDegrees + 360f) % 360f
                        val normalizedTarget = (targetDegrees + 360f) % 360f
                        val distance = abs(
                            (normalizedSweep - normalizedTarget + 540f) % 360f - 180f,
                        )
                        val flare = ((34f - distance) / 34f).coerceIn(0f, 1f)
                        val blipCenter = Offset(
                            center.x + blip.x * unit,
                            center.y + blip.y * unit,
                        )
                        drawCircle(
                            color = accent.copy(alpha = 0.14f + flare * 0.24f),
                            radius = unit * (0.04f + flare * 0.045f),
                            center = blipCenter,
                        )
                        drawCircle(
                            color = accent.copy(alpha = 0.5f + flare * 0.5f),
                            radius = unit * (0.022f + flare * 0.012f),
                            center = blipCenter,
                        )
                    }
                    drawCircle(
                        color = accent,
                        radius = unit * 0.024f,
                        center = center,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackedList(
    items: List<HomeItem>,
    listState: LazyListState,
    onBrowseCatalog: () -> Unit,
    onConceptSelected: (String) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyTrackedState(onBrowseCatalog)
        return
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.concept.id.value }) { item ->
            PriceTickerRow(
                name = item.concept.householdName,
                illustrationAsset = item.concept.illustrationAsset,
                price = item.latestEstimate?.point?.amount,
                changePercent = item.previousChangePercent(),
                onClick = { onConceptSelected(item.concept.id.value) },
            )
        }
    }
}

@Composable
private fun DeclinersList(
    items: List<MarketItem>,
    eligibleCount: Int,
    listState: LazyListState,
    lookbackDays: Int,
    onLookbackSelected: (Int) -> Unit,
    onConceptSelected: (String) -> Unit,
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = 14.dp, bottom = 12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            val options = listOf(1, 3, 7, 14, 30)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                options.forEach { days ->
                    val selected = lookbackDays == days
                    Surface(
                        onClick = { onLookbackSelected(days) },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(11.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                        contentColor = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "$days 日",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
        item {
            Text(
                "跌幅 Top 10 · 最新價較前 $lookbackDays 個交易日均價",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (items.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        if (eligibleCount == 0) {
                            "正在建立 $lookbackDays 日排行"
                        } else {
                            "目前沒有下跌品項"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    if (eligibleCount == 0) {
                        Text(
                            "歷史行情會在連線後自動下載",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(items, key = { it.concept.id.value }) { item ->
                PriceTickerRow(
                    name = item.concept.householdName,
                    illustrationAsset = item.concept.illustrationAsset,
                    price = item.latestEstimate?.point?.amount,
                    changePercent = item.averageChangePercent(lookbackDays),
                    onClick = { onConceptSelected(item.concept.id.value) },
                )
            }
        }
    }
}

@Composable
private fun PriceTickerRow(
    name: String,
    illustrationAsset: String?,
    price: BigDecimal?,
    changePercent: BigDecimal?,
    onClick: () -> Unit,
) {
    val changeText = changePercent.asPercent()
    val changeColor = when {
        changePercent == null || changePercent.signum() == 0 ->
            MaterialTheme.colorScheme.onSurfaceVariant
        changePercent.signum() > 0 -> RisingRed
        else -> FallingGreen
    }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "$name，${price?.setScale(1, RoundingMode.HALF_UP) ?: "無價格"}元每台斤，$changeText"
            },
        color = Color.Transparent,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProduceIllustration(
                    assetPath = illustrationAsset,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
                Text(
                    name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            price?.setScale(1, RoundingMode.HALF_UP)?.toPlainString() ?: "—",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            " / 台斤",
                            modifier = Modifier.padding(bottom = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        changeText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = changeColor,
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 80.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun EmptyTrackedState(onBrowseCatalog: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("建立你的蔬果自選清單", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "加入品項後，就能在這裡快速查看價格與漲跌。",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onBrowseCatalog) { Text("瀏覽蔬菜市場") }
    }
}

private fun BigDecimal?.asPercent(): String {
    if (this == null) return "—"
    val prefix = if (signum() > 0) "+" else ""
    return "$prefix${setScale(1, RoundingMode.HALF_UP).toPlainString()}%"
}
