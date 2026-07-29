package tw.taipei.veges.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.math.BigDecimal
import java.math.RoundingMode
import tw.taipei.veges.designsystem.AiIllustrationDisclosure
import tw.taipei.veges.designsystem.ProduceIllustration
import tw.taipei.veges.domain.MarketItem
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.previousChangePercent

private val MarketRisingRed = Color(0xFFE5484D)
private val MarketFallingGreen = Color(0xFF00A86B)

private enum class VegetableSection(
    val label: String,
    val assetPath: String,
    val codePrefix: Char?,
) {
    LEAFY("葉菜類", "illustrations/categories/leafy.webp", 'L'),
    FRUITING("果菜・花菜・豆類", "illustrations/categories/fruiting.webp", 'F'),
    MUSHROOM("菇類", "illustrations/categories/mushroom.webp", 'M'),
    ROOT_SPROUT("根莖・芽菜類", "illustrations/categories/root-sprout.webp", 'S'),
    PROCESSED("加工蔬菜", "illustrations/categories/processed.webp", 'O'),
    OTHER("其他蔬菜", "illustrations/categories/other.webp", null),
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CatalogRoute(
    category: ProduceCategory,
    onConceptSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CatalogViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(category) { viewModel.selectCategory(category) }
    CatalogScreen(
        state = state,
        onQueryChange = viewModel::updateQuery,
        onCategorySelected = viewModel::selectCategory,
        onConceptSelected = onConceptSelected,
        onDismissAmbiguity = viewModel::clearAmbiguity,
        modifier = modifier,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CatalogScreen(
    state: CatalogUiState,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (ProduceCategory) -> Unit,
    onConceptSelected: (String) -> Unit,
    onDismissAmbiguity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = VegetableSection.entries
    val carouselState = rememberCarouselState { sections.size }
    val selectedSection = sections[carouselState.currentItem.coerceIn(0, sections.lastIndex)]
    val visibleResults = remember(state.results, state.query, selectedSection) {
        if (state.query.isNotBlank() || state.category == ProduceCategory.FRUIT) {
            state.results
        } else {
            state.results.filter { it.section() == selectedSection }
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = 18.dp,
            end = 18.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                if (state.category == ProduceCategory.VEGETABLE) "蔬菜市場" else "水果市場",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
            )
        }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text("搜尋名稱或官方代碼") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.category == ProduceCategory.VEGETABLE && state.query.isBlank()) {
            item {
                HorizontalMultiBrowseCarousel(
                    state = carouselState,
                    preferredItemWidth = 252.dp,
                    itemSpacing = 10.dp,
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(152.dp),
                ) { index ->
                    val section = sections[index]
                    CategoryCarouselCard(
                        section = section,
                        count = state.results.count { it.section() == section },
                        modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge),
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    when {
                        state.query.isNotBlank() -> "搜尋結果"
                        state.category == ProduceCategory.FRUIT -> "全部水果"
                        else -> selectedSection.label
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${visibleResults.size} 項",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (visibleResults.isEmpty()) {
            item { EmptyCatalogState(query = state.query) }
        } else {
            items(
                items = visibleResults,
                key = { it.concept.id.value },
            ) { item ->
                ProduceMarketRow(
                    item = item,
                    onClick = { onConceptSelected(item.concept.id.value) },
                )
            }
        }
        item { AiIllustrationDisclosure() }
    }

    state.ambiguity?.let { ambiguity ->
        ModalBottomSheet(onDismissRequest = onDismissAmbiguity) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "請選擇「${ambiguity.query}」的意思",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                ambiguity.concepts.forEach { concept ->
                    Button(
                        onClick = { onConceptSelected(concept.id.value) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(concept.householdName)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCarouselCard(
    section: VegetableSection,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.extraLarge),
    ) {
        ProduceIllustration(
            assetPath = section.assetPath,
            householdName = section.label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        ) {
            Text(
                section.label,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "$count 項行情",
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun ProduceMarketRow(
    item: MarketItem,
    onClick: () -> Unit,
) {
    val concept = item.concept
    val price = item.latestEstimate?.point?.amount
    val change = item.previousChangePercent()
    val changeText = change.asPercent()
    val changeColor = when {
        change == null || change.signum() == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        change.signum() > 0 -> MarketRisingRed
        else -> MarketFallingGreen
    }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "${concept.householdName}，${price?.setScale(1, RoundingMode.HALF_UP) ?: "無價格"}元每台斤，$changeText"
            },
        color = Color.Transparent,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProduceIllustration(
                    assetPath = concept.illustrationAsset,
                    householdName = concept.householdName,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(15.dp)),
                )
                Text(
                    concept.householdName,
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
                modifier = Modifier.padding(start = 64.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            )
        }
    }
}

private fun MarketItem.section(): VegetableSection {
    val prefix = concept.officialVariants.firstOrNull()?.code?.value?.firstOrNull()
    return VegetableSection.entries.firstOrNull { it.codePrefix == prefix }
        ?: VegetableSection.OTHER
}

@Composable
private fun EmptyCatalogState(query: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Text(
            if (query.isBlank()) "此分類目前沒有行情" else "找不到符合的品項",
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun BigDecimal?.asPercent(): String {
    if (this == null) return "—"
    val prefix = if (signum() > 0) "+" else ""
    return "$prefix${setScale(1, RoundingMode.HALF_UP).toPlainString()}%"
}
