package tw.taipei.veges.home

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.launch
import tw.taipei.veges.designsystem.ProduceIllustration
import tw.taipei.veges.domain.HomeItem
import tw.taipei.veges.domain.MarketItem
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
    Column(modifier.fillMaxSize()) {
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
        PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
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
                    householdName = name,
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
