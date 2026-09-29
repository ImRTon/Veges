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
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DragHandle
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.zIndex
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
import tw.taipei.veges.designsystem.PillChoiceRow
import tw.taipei.veges.designsystem.ProduceIllustration
import tw.taipei.veges.domain.HomeItem
import tw.taipei.veges.domain.MarketItem
import tw.taipei.veges.domain.MarketPriceSurgeOutlook
import tw.taipei.veges.domain.MarketShockKind
import tw.taipei.veges.domain.PriceSurgeReason
import tw.taipei.veges.domain.PriceSurgeReasonKind
import tw.taipei.veges.domain.PriceSurgeRiskLevel
import tw.taipei.veges.domain.ProductionAreaWeatherRisk
import tw.taipei.veges.domain.PriceRefresh
import tw.taipei.veges.domain.PriceRefreshStage
import tw.taipei.veges.domain.averageChangePercent
import tw.taipei.veges.domain.previousChangePercent

private val RisingRed = Color(0xFFE5484D)
private val FallingGreen = Color(0xFF00A86B)
private val HomeTabs = listOf("追蹤", "大跌")
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
        onMoveTrackedItem = viewModel::moveTrackedItem,
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
    onMoveTrackedItem: (draggedConceptId: String, targetConceptId: String) -> Unit = { _, _ -> },
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
        Column(
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
        ) {
            PriceSurgeRadarSection(
                outlook = state.marketPriceSurgeOutlook,
                eligibleItemCount = state.predictionEligibleCount,
                productionAreaWeatherRisk = state.productionAreaWeatherRisk,
            )
        }
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
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
                    onMoveItem = onMoveTrackedItem,
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
    productionAreaWeatherRisk: ProductionAreaWeatherRisk?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "漲價雷達",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                "預測 7–14 日",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (outlook == null && productionAreaWeatherRisk != null) {
            ProductionAreaWeatherRiskRadar(productionAreaWeatherRisk)
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
                                "已分析 $eligibleItemCount 項蔬果"
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
                "至少 10 項具足夠歷史，且 20% 以上同步承壓才顯示",
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProductionAreaWeatherRiskRadar(risk: ProductionAreaWeatherRisk) {
    val cause = when (risk.kind) {
        MarketShockKind.TYPHOON -> "颱風"
        MarketShockKind.HEAVY_RAIN -> "豪雨"
        MarketShockKind.EXTREME_HEAT -> "高溫"
    }
    val areaSummary = when (risk.affectedCounties.size) {
        1 -> "${risk.affectedCounties.single()}產區"
        2, 3 -> "${risk.affectedCounties.joinToString("、")}產區"
        else -> "${risk.affectedCounties.take(3).joinToString("、")}等 ${risk.affectedCounties.size} 個產區"
    }
    val headline = "${cause}影響$areaSummary"
    val consequence = "近期蔬果價格可能上漲"
    val reason = PriceSurgeReason(
        kind = when (risk.kind) {
            MarketShockKind.TYPHOON -> PriceSurgeReasonKind.TYPHOON
            MarketShockKind.HEAVY_RAIN -> PriceSurgeReasonKind.HEAVY_RAIN
            MarketShockKind.EXTREME_HEAT -> PriceSurgeReasonKind.EXTREME_HEAT
        },
        contribution = 0,
        headline = headline,
        shortLabel = cause,
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$headline。$consequence"
            },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedRiskIcon(
                reason = reason,
                modifier = Modifier.size(60.dp),
                accent = reason.kind.animationAccent(),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    headline,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    consequence,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
private fun PriceSurgeReasonKind.animationAccent(): Color = when (this) {
    PriceSurgeReasonKind.TYPHOON -> MaterialTheme.colorScheme.secondary
    PriceSurgeReasonKind.HEAVY_RAIN -> Color(0xFF4F8DB8)
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
                    PriceSurgeReasonKind.TYPHOON -> 9_600
                    PriceSurgeReasonKind.HEAVY_RAIN -> 1_600
                    PriceSurgeReasonKind.EXTREME_HEAT -> 4_200
                    PriceSurgeReasonKind.VOLUME_CONTRACTION -> 2_800
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
        color = if (reason.kind == PriceSurgeReasonKind.TYPHOON) {
            Color.Transparent
        } else {
            accent.copy(alpha = 0.105f)
        },
        contentColor = accent,
        border = if (reason.kind == PriceSurgeReasonKind.TYPHOON) {
            null
        } else {
            BorderStroke(
                width = 1.dp,
                color = accent.copy(alpha = 0.16f),
            )
        },
    ) {
        if (reason.kind == PriceSurgeReasonKind.TYPHOON) {
            AnimatedTyphoon(
                progress = progress,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
            )
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(7.dp),
            ) {
            val unit = size.minDimension
            val center = Offset(size.width / 2f, size.height / 2f)
            val tau = (PI * 2).toFloat()
            if (
                reason.kind != PriceSurgeReasonKind.TYPHOON &&
                reason.kind != PriceSurgeReasonKind.HEAVY_RAIN &&
                reason.kind != PriceSurgeReasonKind.EXTREME_HEAT
            ) {
                drawCircle(
                    color = accent.copy(alpha = 0.045f),
                    radius = unit * 0.48f,
                    center = center,
                )
            }
            when (reason.kind) {
                PriceSurgeReasonKind.HEAVY_RAIN -> {
                    drawRealisticHeavyRain(progress = progress, unit = unit)
                }

                PriceSurgeReasonKind.TYPHOON -> {
                    Unit
                }

                PriceSurgeReasonKind.EXTREME_HEAT -> {
                    drawRealisticExtremeHeat(progress = progress, unit = unit)
                }

                PriceSurgeReasonKind.VOLUME_CONTRACTION -> {
                    drawShrinkingCargoBox(
                        progress = progress,
                        unit = unit,
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
}

@Composable
private fun AnimatedTyphoon(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val tau = (PI * 2).toFloat()
    val layerTransition = rememberInfiniteTransition(label = "typhoon-layers")
    val outerProgress by layerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "typhoon-outer",
    )
    val innerProgress by layerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "typhoon-inner",
    )
    val outerWave = (sin(outerProgress * tau + tau * 0.68f) + 1f) / 2f
    val middleWave = sin(progress * tau * 2f + tau / 3f)
    val innerWave = sin(innerProgress * tau * 2f)

    Box(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.typhoon_material_outer),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val outwardScale = 1f + outerWave * 0.028f
                    rotationZ = -outerProgress * 360f - outerWave * 3.2f
                    scaleX = outwardScale
                    scaleY = outwardScale
                    alpha = 0.9f + outerWave * 0.1f
                },
        )
        Image(
            painter = painterResource(R.drawable.typhoon_material_middle),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val breathingScale = 1f + middleWave * 0.009f
                    rotationZ = -progress * 360f + middleWave * 1.7f
                    scaleX = breathingScale
                    scaleY = breathingScale
                    alpha = 0.97f + middleWave * 0.03f
                },
        )
        Image(
            painter = painterResource(R.drawable.typhoon_material_inner),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val breathingScale = 1f + innerWave * 0.006f
                    rotationZ = -innerProgress * 360f - innerWave * 2.4f
                    scaleX = breathingScale
                    scaleY = breathingScale
                    alpha = 0.985f + innerWave * 0.015f
                },
        )
    }
}

private fun DrawScope.drawRealisticTyphoon(
    progress: Float,
    unit: Float,
    accent: Color,
) {
    val tau = (PI * 2).toFloat()
    val stormCenter = Offset(size.width * 0.5f, size.height * 0.5f)
    val rotation = -progress * tau
    val cloudLight = Color(0xFFEAF8F5)
    val cloudMid = Color(0xFF9FD2C7)
    val stormDeep = Color(0xFF173F45)

    drawCircle(
        brush = Brush.radialGradient(
            0f to accent.copy(alpha = 0.2f),
            0.4f to accent.copy(alpha = 0.09f),
            0.74f to cloudMid.copy(alpha = 0.055f),
            1f to Color.Transparent,
            center = stormCenter,
            radius = unit * 0.5f,
        ),
        radius = unit * 0.5f,
        center = stormCenter,
    )

    repeat(4) { band ->
        val bandOffset = when (band) {
            0 -> 0f
            1 -> 1.48f
            2 -> 3.18f
            else -> 4.92f
        }
        val bandReach = when (band) {
            0 -> 0.37f
            1 -> 0.33f
            2 -> 0.35f
            else -> 0.29f
        }
        val bandTurns = when (band) {
            0 -> 0.7f
            1 -> 0.62f
            2 -> 0.74f
            else -> 0.56f
        }
        val bandStart = rotation + bandOffset
        val bandWobble = sin(progress * tau * 2f + band * 1.7f) * 0.035f
        drawTyphoonBandSection(
            center = stormCenter,
            unit = unit,
            startAngle = bandStart,
            startFraction = 0f,
            endFraction = 0.44f,
            reach = bandReach,
            turns = bandTurns + bandWobble,
            width = unit * 0.086f,
            accent = accent,
            cloudMid = cloudMid,
            cloudLight = cloudLight,
            alpha = 0.82f,
        )
        drawTyphoonBandSection(
            center = stormCenter,
            unit = unit,
            startAngle = bandStart,
            startFraction = 0.34f,
            endFraction = 0.76f,
            reach = bandReach,
            turns = bandTurns + bandWobble,
            width = unit * 0.05f,
            accent = accent,
            cloudMid = cloudMid,
            cloudLight = cloudLight,
            alpha = 0.7f,
        )
        drawTyphoonBandSection(
            center = stormCenter,
            unit = unit,
            startAngle = bandStart,
            startFraction = 0.67f,
            endFraction = 1f,
            reach = bandReach,
            turns = bandTurns + bandWobble,
            width = unit * 0.023f,
            accent = accent,
            cloudMid = cloudMid,
            cloudLight = cloudLight,
            alpha = 0.56f,
        )
    }

    repeat(12) { index ->
        val travel = (progress * 2f + index / 12f) % 1f
        val arm = index % 4
        val angle = rotation + arm * tau / 4f + travel * tau * 0.68f
        val radius = unit * (0.19f + travel * 0.27f)
        val particleCenter = Offset(
            x = stormCenter.x + cos(angle).toFloat() * radius,
            y = stormCenter.y + sin(angle).toFloat() * radius * 0.94f,
        )
        val tangent = angle + PI.toFloat() / 2f
        val length = unit * (0.018f + (1f - travel) * 0.032f)
        val fade = sin(travel * PI).toFloat().coerceAtLeast(0f)
        drawLine(
            color = cloudLight.copy(alpha = fade * 0.68f),
            start = Offset(
                x = particleCenter.x - cos(tangent).toFloat() * length,
                y = particleCenter.y - sin(tangent).toFloat() * length,
            ),
            end = Offset(
                x = particleCenter.x + cos(tangent).toFloat() * length,
                y = particleCenter.y + sin(tangent).toFloat() * length,
            ),
            strokeWidth = unit * 0.012f,
            cap = StrokeCap.Round,
        )
    }

    val eyePulse = 1f + sin(progress * tau * 2f) * 0.025f
    drawCircle(
        color = accent.copy(alpha = 0.28f),
        radius = unit * 0.155f * eyePulse,
        center = stormCenter,
    )
    drawCircle(
        brush = Brush.radialGradient(
            0f to cloudLight.copy(alpha = 0.9f),
            0.58f to cloudLight.copy(alpha = 0.82f),
            1f to cloudMid.copy(alpha = 0.66f),
            center = stormCenter,
            radius = unit * 0.12f,
        ),
        radius = unit * 0.12f * eyePulse,
        center = stormCenter,
    )
    val eyeCenter = Offset(
        x = stormCenter.x + unit * 0.006f,
        y = stormCenter.y + unit * 0.004f,
    )
    drawOval(
        brush = Brush.radialGradient(
            0f to stormDeep.copy(alpha = 0.98f),
            0.7f to stormDeep.copy(alpha = 0.9f),
            1f to accent.copy(alpha = 0.72f),
            center = eyeCenter,
            radius = unit * 0.058f,
        ),
        topLeft = Offset(
            x = eyeCenter.x - unit * 0.055f,
            y = eyeCenter.y - unit * 0.046f,
        ),
        size = Size(unit * 0.11f, unit * 0.092f),
    )
    drawArc(
        color = cloudLight.copy(alpha = 0.7f),
        startAngle = 196f - progress * 360f,
        sweepAngle = 86f,
        useCenter = false,
        topLeft = Offset(
            x = eyeCenter.x - unit * 0.068f,
            y = eyeCenter.y - unit * 0.06f,
        ),
        size = Size(unit * 0.136f, unit * 0.12f),
        style = Stroke(
            width = unit * 0.012f,
            cap = StrokeCap.Round,
        ),
    )
}

private fun DrawScope.drawTyphoonBandSection(
    center: Offset,
    unit: Float,
    startAngle: Float,
    startFraction: Float,
    endFraction: Float,
    reach: Float,
    turns: Float,
    width: Float,
    accent: Color,
    cloudMid: Color,
    cloudLight: Color,
    alpha: Float,
) {
    val path = Path()
    repeat(14) { step ->
        val fraction = startFraction + (endFraction - startFraction) * step / 13f
        val point = typhoonBandPoint(
            center = center,
            unit = unit,
            startAngle = startAngle,
            fraction = fraction,
            reach = reach,
            turns = turns,
        )
        if (step == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    drawPath(
        path = path,
        color = accent.copy(alpha = alpha * 0.3f),
        style = Stroke(width = width * 1.46f, cap = StrokeCap.Round),
    )
    drawPath(
        path = path,
        color = cloudMid.copy(alpha = alpha),
        style = Stroke(width = width, cap = StrokeCap.Round),
    )
    drawPath(
        path = path,
        color = cloudLight.copy(alpha = alpha * 0.74f),
        style = Stroke(width = width * 0.34f, cap = StrokeCap.Round),
    )
}

private fun typhoonBandPoint(
    center: Offset,
    unit: Float,
    startAngle: Float,
    fraction: Float,
    reach: Float,
    turns: Float,
): Offset {
    val tau = (PI * 2).toFloat()
    val eased = fraction * fraction * (3f - 2f * fraction)
    val radius = unit * (0.105f + reach * eased)
    val angle = startAngle + fraction * tau * turns
    return Offset(
        x = center.x + cos(angle).toFloat() * radius,
        y = center.y + sin(angle).toFloat() * radius * 0.94f,
    )
}

private fun DrawScope.drawShrinkingCargoBox(
    progress: Float,
    unit: Float,
) {
    val tau = (PI * 2).toFloat()
    val shrink = (1f - cos(progress * tau)) / 2f
    val scale = 1f - shrink * 0.46f
    val boxWidth = unit * 0.61f * scale
    val boxHeight = unit * 0.5f * scale
    val boxLeft = size.width * 0.5f - boxWidth / 2f
    val boxTop = size.height * 0.52f - boxHeight / 2f
    val lidHeight = boxHeight * 0.22f
    val corner = unit * 0.025f * scale
    val cardboard = Color(0xFFC89455)
    val cardboardDark = Color(0xFF8B5A2B)
    val tape = Color(0xFFE9D2A9)

    drawRoundRect(
        color = cardboard,
        topLeft = Offset(boxLeft, boxTop + lidHeight * 0.62f),
        size = Size(boxWidth, boxHeight - lidHeight * 0.62f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
    )
    val lid = Path().apply {
        moveTo(boxLeft + boxWidth * 0.05f, boxTop)
        lineTo(boxLeft + boxWidth * 0.95f, boxTop)
        lineTo(boxLeft + boxWidth, boxTop + lidHeight)
        lineTo(boxLeft, boxTop + lidHeight)
        close()
    }
    drawPath(path = lid, color = Color(0xFFD9AA6D))
    drawPath(
        path = lid,
        color = cardboardDark,
        style = Stroke(width = unit * 0.022f * scale, cap = StrokeCap.Round),
    )
    drawRoundRect(
        color = cardboardDark,
        topLeft = Offset(boxLeft, boxTop + lidHeight * 0.62f),
        size = Size(boxWidth, boxHeight - lidHeight * 0.62f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
        style = Stroke(width = unit * 0.022f * scale),
    )
    drawLine(
        color = cardboardDark.copy(alpha = 0.62f),
        start = Offset(boxLeft, boxTop + lidHeight),
        end = Offset(boxLeft + boxWidth, boxTop + lidHeight),
        strokeWidth = unit * 0.017f * scale,
        cap = StrokeCap.Round,
    )
    val tapeWidth = boxWidth * 0.13f
    drawRect(
        color = tape.copy(alpha = 0.82f),
        topLeft = Offset(boxLeft + (boxWidth - tapeWidth) / 2f, boxTop),
        size = Size(tapeWidth, boxHeight),
    )
    drawLine(
        color = cardboardDark.copy(alpha = 0.3f),
        start = Offset(boxLeft + boxWidth / 2f, boxTop),
        end = Offset(boxLeft + boxWidth / 2f, boxTop + boxHeight),
        strokeWidth = unit * 0.008f * scale,
    )
}

private fun DrawScope.drawRealisticHeavyRain(
    progress: Float,
    unit: Float,
) {
    val tau = (PI * 2).toFloat()
    val groundY = size.height * 0.86f

    val cloudDrift = sin(progress * tau) * unit * 0.012f
    val cloudTop = size.height * 0.08f
    drawOval(
        brush = Brush.verticalGradient(
            0f to Color(0xFF9DA9B0),
            0.42f to Color(0xFF687985),
            1f to Color(0xFF334956),
            startY = cloudTop,
            endY = size.height * 0.43f,
        ),
        topLeft = Offset(size.width * 0.09f + cloudDrift, size.height * 0.2f),
        size = Size(size.width * 0.84f, unit * 0.22f),
    )
    drawCircle(
        color = Color(0xFF71818B),
        radius = unit * 0.16f,
        center = Offset(size.width * 0.3f + cloudDrift, size.height * 0.23f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFAAB2B6),
            0.62f to Color(0xFF778690),
            1f to Color(0xFF566A77),
            center = Offset(size.width * 0.52f + cloudDrift, size.height * 0.18f),
            radius = unit * 0.23f,
        ),
        radius = unit * 0.21f,
        center = Offset(size.width * 0.52f + cloudDrift, size.height * 0.2f),
    )
    drawCircle(
        color = Color(0xFF657783),
        radius = unit * 0.14f,
        center = Offset(size.width * 0.72f + cloudDrift, size.height * 0.25f),
    )
    drawOval(
        color = Color(0xFF243B49).copy(alpha = 0.7f),
        topLeft = Offset(size.width * 0.16f + cloudDrift, size.height * 0.33f),
        size = Size(size.width * 0.7f, unit * 0.09f),
    )

    val rainTop = size.height * 0.49f
    repeat(18) { index ->
        val cycles = 2f + (index % 3)
        val fall = (progress * cycles + index * 0.137f) % 1f
        val lane = (index * 0.6180339f) % 1f
        val depth = 0.58f + (index % 5) * 0.1f
        val x = size.width * (0.12f + lane * 0.8f) + fall * unit * 0.02f
        val y = rainTop + fall * (groundY - rainTop)
        val length = unit * (0.075f + depth * 0.075f)
        val rainEndY = (y + length).coerceAtMost(groundY)
        val rainEndX = x - (rainEndY - y) * 0.38f
        drawLine(
            color = Color(0xFFD9F0FA).copy(alpha = 0.25f + depth * 0.58f),
            start = Offset(x, y),
            end = Offset(rainEndX, rainEndY),
            strokeWidth = unit * (0.009f + depth * 0.011f),
            cap = StrokeCap.Round,
        )
        if (fall > 0.86f) {
            val splash = (fall - 0.86f) / 0.14f
            val splashAlpha = sin(splash * PI).toFloat().coerceAtLeast(0f) * depth
            val splashX = rainEndX
            drawLine(
                color = Color(0xFFD9F0FA).copy(alpha = splashAlpha * 0.58f),
                start = Offset(splashX, groundY),
                end = Offset(
                    splashX - unit * 0.035f * splash,
                    groundY - unit * 0.035f * splash,
                ),
                strokeWidth = unit * 0.009f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color(0xFFD9F0FA).copy(alpha = splashAlpha * 0.45f),
                start = Offset(splashX, groundY),
                end = Offset(
                    splashX + unit * 0.045f * splash,
                    groundY - unit * 0.025f * splash,
                ),
                strokeWidth = unit * 0.008f,
                cap = StrokeCap.Round,
            )
        }
    }

    drawOval(
        brush = Brush.horizontalGradient(
            0f to Color.Transparent,
            0.5f to Color(0xFFBED7E3).copy(alpha = 0.28f),
            1f to Color.Transparent,
        ),
        topLeft = Offset(size.width * 0.04f, groundY - unit * 0.018f),
        size = Size(size.width * 0.9f, unit * 0.055f),
    )
    repeat(3) { index ->
        val ripple = (progress * 2f + index * 0.34f) % 1f
        val rippleWidth = unit * (0.08f + ripple * 0.17f)
        drawOval(
            color = Color(0xFFD9F0FA).copy(alpha = (1f - ripple) * 0.28f),
            topLeft = Offset(
                size.width * (0.28f + index * 0.22f) - rippleWidth / 2f,
                groundY - unit * 0.012f,
            ),
            size = Size(rippleWidth, unit * 0.035f),
            style = Stroke(width = unit * 0.009f),
        )
    }
}

private fun DrawScope.drawRealisticExtremeHeat(
    progress: Float,
    unit: Float,
) {
    val tau = (PI * 2).toFloat()
    val sunCenter = Offset(size.width * 0.62f, size.height * 0.3f)
    val pulse = 1f + sin(progress * tau) * 0.025f
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFFFF5D2).copy(alpha = 0.38f),
            0.4f to Color(0xFFFFC76B).copy(alpha = 0.2f),
            1f to Color.Transparent,
            center = sunCenter,
            radius = unit * 0.36f,
        ),
        radius = unit * 0.36f,
        center = sunCenter,
    )
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFFFFBE8),
            0.72f to Color(0xFFFFD88D),
            1f to Color(0xFFF29C45),
            center = Offset(
                sunCenter.x - unit * 0.035f,
                sunCenter.y - unit * 0.035f,
            ),
            radius = unit * 0.17f,
        ),
        radius = unit * 0.155f * pulse,
        center = sunCenter,
    )

    val horizonY = size.height * 0.73f
    drawOval(
        brush = Brush.verticalGradient(
            0f to Color(0xFF6E4930).copy(alpha = 0.34f),
            1f to Color(0xFF241B18).copy(alpha = 0.82f),
            startY = horizonY,
            endY = size.height * 1.08f,
        ),
        topLeft = Offset(-size.width * 0.08f, horizonY),
        size = Size(size.width * 1.16f, size.height * 0.38f),
    )
    repeat(5) { row ->
        val path = Path()
        repeat(24) { step ->
            val fraction = step / 23f
            val x = size.width * (0.06f + fraction * 0.88f)
            val baseY = size.height * (0.5f + row * 0.065f)
            val wave = sin(
                fraction * tau * (1.7f + row * 0.08f) + progress * tau + row * 0.9f,
            ) * unit * (0.008f + row * 0.0015f)
            val y = baseY + wave
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = Color(0xFFFFE2A8).copy(alpha = 0.12f + row * 0.035f),
            style = Stroke(
                width = unit * (0.01f + row * 0.001f),
                cap = StrokeCap.Round,
            ),
        )
    }
    repeat(4) { index ->
        val dustProgress = (progress + index * 0.25f) % 1f
        drawCircle(
            color = Color(0xFFFFD08A).copy(
                alpha = sin(dustProgress * PI).toFloat().coerceAtLeast(0f) * 0.2f,
            ),
            radius = unit * (0.008f + index * 0.002f),
            center = Offset(
                size.width * (0.16f + index * 0.19f) + dustProgress * unit * 0.04f,
                horizonY - dustProgress * unit * 0.18f,
            ),
        )
    }
}

@Composable
private fun TrackedList(
    items: List<HomeItem>,
    listState: LazyListState,
    onBrowseCatalog: () -> Unit,
    onConceptSelected: (String) -> Unit,
    onMoveItem: (draggedConceptId: String, targetConceptId: String) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyTrackedState(onBrowseCatalog)
        return
    }
    var dragPreviewIds by remember { mutableStateOf<List<String>?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val canReorder = items.size > 1
    val currentOnMoveItem = rememberUpdatedState(onMoveItem)
    val previewMoveHandler = rememberUpdatedState<(String, String) -> Unit> { draggedId, targetId ->
        val currentIds = dragPreviewIds ?: items.map { it.concept.id.value }
        currentIds.move(draggedId, targetId)?.let { reorderedIds ->
            dragPreviewIds = reorderedIds
            currentOnMoveItem.value(draggedId, targetId)
        }
    }
    val dragDropState = remember(listState, coroutineScope) {
        HomeDragDropState(listState, coroutineScope) { draggedId, targetId ->
            previewMoveHandler.value(draggedId, targetId)
        }
    }
    val displayedItems = remember(items, dragPreviewIds) {
        dragPreviewIds?.let { savedIds -> items.orderedBySavedIds(savedIds) } ?: items
    }
    LaunchedEffect(items, dragDropState.isDragging, dragPreviewIds) {
        val persistedIds = items.map { it.concept.id.value }
        if (!dragDropState.isDragging && dragPreviewIds == persistedIds) {
            dragPreviewIds = null
        }
    }
    val hapticFeedback = LocalHapticFeedback.current
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(dragDropState, canReorder) {
                if (!canReorder) return@pointerInput
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        if (dragDropState.onDragStart(offset.y)) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (dragDropState.isDragging) {
                            change.consume()
                            dragDropState.onDrag(dragAmount.y)
                        }
                    },
                    onDragEnd = dragDropState::onDragEnd,
                    onDragCancel = dragDropState::onDragEnd,
                )
            },
    ) {
        if (canReorder) {
            item {
                Text(
                    "長按並拖曳可調整順序",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(
            items = displayedItems,
            key = { _, item -> homeItemKey(item.concept.id.value) },
        ) { index, item ->
            val conceptId = item.concept.id.value
            val isDragging = dragDropState.draggedConceptId == conceptId
            PriceTickerRow(
                name = item.concept.householdName,
                illustrationAsset = item.concept.illustrationAsset,
                price = item.latestEstimate?.point?.amount,
                changePercent = item.previousChangePercent(),
                onClick = { onConceptSelected(conceptId) },
                canReorder = canReorder,
                isDragging = isDragging,
                onMoveUp = displayedItems.getOrNull(index - 1)?.let { previous ->
                    { previewMoveHandler.value(conceptId, previous.concept.id.value) }
                },
                onMoveDown = displayedItems.getOrNull(index + 1)?.let { next ->
                    { previewMoveHandler.value(conceptId, next.concept.id.value) }
                },
                modifier = (if (isDragging) Modifier else Modifier.animateItem())
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        if (isDragging) {
                            translationY = dragDropState.draggedItemOffset
                            scaleX = 1.02f
                            scaleY = 1.02f
                            shadowElevation = 10.dp.toPx()
                        }
                    },
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
            PillChoiceRow(
                items = options,
                selectedItem = lookbackDays,
                onItemSelected = onLookbackSelected,
                itemLabel = { "$it 日" },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedContentColor = MaterialTheme.colorScheme.onPrimary,
            )
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
    canReorder: Boolean = false,
    isDragging: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
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
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "$name，${price?.setScale(1, RoundingMode.HALF_UP) ?: "無價格"}元每台斤，$changeText" +
                        if (canReorder) "，長按並拖曳可調整順序" else ""
                customActions = buildList {
                    onMoveUp?.let { moveUp ->
                        add(CustomAccessibilityAction("往上移") { moveUp(); true })
                    }
                    onMoveDown?.let { moveDown ->
                        add(CustomAccessibilityAction("往下移") { moveDown(); true })
                    }
                }
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
                if (canReorder && isDragging) {
                    Icon(
                        imageVector = Icons.Rounded.DragHandle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
