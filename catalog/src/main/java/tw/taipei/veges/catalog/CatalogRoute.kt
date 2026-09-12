package tw.taipei.veges.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import tw.taipei.veges.designsystem.ProduceIllustration
import tw.taipei.veges.domain.MarketItem
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.previousChangePercent

private val MarketRisingRed = Color(0xFFE5484D)
private val MarketFallingGreen = Color(0xFF00A86B)

private sealed interface CatalogSection {
    val label: String
    val assetPath: String

    fun contains(item: MarketItem): Boolean
}

private enum class VegetableSection(
    override val label: String,
    override val assetPath: String,
    private val codePrefix: Char?,
) : CatalogSection {
    LEAFY("葉菜類", "illustrations/categories/leafy.webp", 'L'),
    FRUITING("果菜・花菜・豆類", "illustrations/categories/fruiting.webp", 'F'),
    MUSHROOM("菇類", "illustrations/categories/mushroom.webp", 'M'),
    ROOT_SPROUT("根莖・芽菜類", "illustrations/categories/root-sprout.webp", 'S'),
    PROCESSED("加工蔬菜", "illustrations/categories/processed.webp", 'O'),
    OTHER("其他蔬菜", "illustrations/categories/other.webp", null);

    override fun contains(item: MarketItem): Boolean {
        val prefix = item.concept.officialVariants.firstOrNull()?.code?.value?.firstOrNull()
        return if (this == OTHER) {
            entries.none { it.codePrefix != null && it.codePrefix == prefix }
        } else {
            codePrefix == prefix
        }
    }
}

private enum class FruitSection(
    override val label: String,
    override val assetPath: String,
    val conceptIds: Set<String>,
) : CatalogSection {
    CITRUS(
        "柑橘類",
        "illustrations/categories/fruit-citrus.webp",
        setOf(
            "fruit.mandarin",
            "fruit.tankan",
            "fruit.orange",
            "fruit.mixed-citrus",
            "fruit.lemon",
            "fruit.kumquat",
            "fruit.pomelo",
            "fruit.grapefruit",
        ),
    ),
    MELON(
        "瓜果類",
        "illustrations/categories/fruit-melon.webp",
        setOf(
            "fruit.watermelon",
            "fruit.oriental-melon",
            "fruit.muskmelon",
            "fruit.pepino",
        ),
    ),
    TROPICAL(
        "熱帶水果",
        "illustrations/categories/fruit-tropical.webp",
        setOf(
            "fruit.coconut",
            "fruit.passion-fruit",
            "fruit.dragon-fruit",
            "fruit.durian",
            "fruit.mangosteen",
            "fruit.rambutan",
            "fruit.banana",
            "fruit.pineapple",
            "fruit.eggfruit",
            "fruit.abiu",
            "fruit.avocado",
            "fruit.jackfruit",
            "fruit.cempedak",
            "fruit.papaya",
            "fruit.mango",
        ),
    ),
    ORCHARD(
        "果園・堅果",
        "illustrations/categories/fruit-orchard.webp",
        setOf(
            "fruit.kiwi",
            "fruit.chestnut",
            "fruit.loquat",
            "fruit.pear",
            "fruit.apple",
            "fruit.persimmon",
        ),
    ),
    STONE_FRUIT(
        "桃李・梅棗",
        "illustrations/categories/fruit-stone.webp",
        setOf(
            "fruit.jujube",
            "fruit.honey-jujube",
            "fruit.ume",
            "fruit.chinese-bayberry",
            "fruit.cherry",
            "fruit.olive",
            "fruit.plum",
            "fruit.peach",
        ),
    ),
    BERRY_GRAPE(
        "莓果・葡萄",
        "illustrations/categories/fruit-berry.webp",
        setOf(
            "fruit.mulberry",
            "fruit.strawberry",
            "fruit.blueberry",
            "fruit.cherry-tomato",
            "fruit.jabuticaba",
            "fruit.grape",
        ),
    ),
    TAIWAN_SPECIALTY(
        "台灣特色・其他",
        "illustrations/categories/fruit-taiwan.webp",
        setOf(
            "fruit.custard-apple",
            "fruit.sugarcane",
            "fruit.lychee",
            "fruit.longan",
            "fruit.starfruit",
            "fruit.guava",
            "fruit.wax-apple",
            "fruit.other",
        ),
    );

    override fun contains(item: MarketItem): Boolean =
        item.concept.id.value in conceptIds
}

internal val fruitSectionByConceptId: Map<String, String> = buildMap {
    FruitSection.entries.forEach { section ->
        section.conceptIds.forEach { conceptId ->
            check(put(conceptId, section.label) == null) {
                "Fruit concept $conceptId appears in more than one Carousel section"
            }
        }
    }
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
        onMoveItem = viewModel::moveItem,
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
    onMoveItem: (draggedConceptId: String, targetConceptId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val sections: List<CatalogSection> = remember(state.category) {
        when (state.category) {
            ProduceCategory.VEGETABLE -> VegetableSection.entries
            ProduceCategory.FRUIT -> FruitSection.entries
        }.toList()
    }
    val carouselState = rememberCarouselState { sections.size }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val sectionSwipeState = remember(carouselState, coroutineScope, sections.size) {
        CatalogSectionSwipeState(carouselState, coroutineScope, sections.size)
    }
    LaunchedEffect(carouselState.currentItem) {
        sectionSwipeState.syncFromCarousel()
    }
    val selectedSectionIndex = sectionSwipeState.selectedSectionIndex
    val selectedSection = sections[selectedSectionIndex]
    val visibleResults = remember(state.results, state.query, selectedSection) {
        if (state.query.isNotBlank()) {
            state.results
        } else {
            state.results.filter(selectedSection::contains)
        }
    }
    var dragPreviewIds by remember { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(state.category, selectedSection, state.query) {
        dragPreviewIds = null
    }
    val displayedResults = remember(visibleResults, dragPreviewIds) {
        dragPreviewIds?.let(visibleResults::orderedByConceptIds) ?: visibleResults
    }
    val canReorder = state.query.isBlank() && visibleResults.size > 1
    val currentOnMoveItem = rememberUpdatedState(onMoveItem)
    val previewMoveHandler = rememberUpdatedState<(String, String) -> Unit> { draggedId, targetId ->
        val currentIds = dragPreviewIds ?: visibleResults.map { it.concept.id.value }
        currentIds.move(draggedId, targetId)?.let { reorderedIds ->
            dragPreviewIds = reorderedIds
            currentOnMoveItem.value(draggedId, targetId)
        }
    }
    val dragDropState = remember(lazyListState, coroutineScope) {
        CatalogDragDropState(lazyListState, coroutineScope) { draggedId, targetId ->
            previewMoveHandler.value(draggedId, targetId)
        }
    }
    LaunchedEffect(visibleResults, dragDropState.isDragging, dragPreviewIds) {
        val persistedIds = visibleResults.map { it.concept.id.value }
        if (!dragDropState.isDragging && dragPreviewIds == persistedIds) {
            dragPreviewIds = null
        }
    }
    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(selectedSection) {
        if (lazyListState.firstVisibleItemIndex > CatalogResultsHeaderIndex) {
            lazyListState.scrollToItem(CatalogResultsHeaderIndex)
        }
    }
    LazyColumn(
        state = lazyListState,
        modifier = modifier
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
                placeholder = { Text("搜尋蔬果名稱") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { keyboardController?.hide() },
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalog-search"),
            )
        }
        if (state.query.isBlank()) {
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
                        count = state.results.count(section::contains),
                        modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge),
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .swipeToChangeSection(
                        enabled = state.query.isBlank(),
                        state = sectionSwipeState,
                    )
                    .semantics {
                        customActions = buildList {
                            sections.getOrNull(selectedSectionIndex - 1)?.let { previous ->
                                add(
                                    CustomAccessibilityAction("切到${previous.label}") {
                                        sectionSwipeState.animateBy(-1)
                                        true
                                    },
                                )
                            }
                            sections.getOrNull(selectedSectionIndex + 1)?.let { next ->
                                add(
                                    CustomAccessibilityAction("切到${next.label}") {
                                        sectionSwipeState.animateBy(1)
                                        true
                                    },
                                )
                            }
                        }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    when {
                        state.query.isNotBlank() -> "搜尋結果"
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
            item {
                EmptyCatalogState(
                    query = state.query,
                    modifier = Modifier.swipeToChangeSection(
                        enabled = state.query.isBlank(),
                        state = sectionSwipeState,
                    ),
                )
            }
        } else {
            itemsIndexed(
                items = displayedResults,
                key = { _, item -> catalogItemKey(item.concept.id.value) },
            ) { index, item ->
                val conceptId = item.concept.id.value
                val isDragging = dragDropState.draggedConceptId == conceptId
                ProduceMarketRow(
                    item = item,
                    onClick = { onConceptSelected(conceptId) },
                    canReorder = canReorder,
                    isDragging = isDragging,
                    onMoveUp = displayedResults.getOrNull(index - 1)?.let { previous ->
                        { previewMoveHandler.value(conceptId, previous.concept.id.value) }
                    },
                    onMoveDown = displayedResults.getOrNull(index + 1)?.let { next ->
                        { previewMoveHandler.value(conceptId, next.concept.id.value) }
                    },
                    modifier = (if (isDragging) Modifier else Modifier.animateItem())
                        .swipeToChangeSection(
                            enabled = state.query.isBlank(),
                            state = sectionSwipeState,
                        )
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
    section: CatalogSection,
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
    canReorder: Boolean,
    isDragging: Boolean,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    modifier: Modifier = Modifier,
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
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "${concept.householdName}，${price?.setScale(1, RoundingMode.HALF_UP) ?: "無價格"}元每台斤，$changeText" +
                        if (canReorder) "，長按可拖曳排序" else ""
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
                modifier = Modifier.padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProduceIllustration(
                    assetPath = concept.illustrationAsset,
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
                modifier = Modifier.padding(start = 64.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun EmptyCatalogState(
    query: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
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

private fun Modifier.swipeToChangeSection(
    enabled: Boolean,
    state: CatalogSectionSwipeState,
): Modifier = if (!enabled) {
    this
} else {
    pointerInput(state) {
        var horizontalDistance = 0f
        detectHorizontalDragGestures(
            onDragStart = {
                horizontalDistance = 0f
                state.startGesture()
            },
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                horizontalDistance += dragAmount
                state.dragBy(dragAmount)
            },
            onDragEnd = {
                state.finishGesture(
                    horizontalDistance = horizontalDistance,
                    threshold = CatalogSectionSwipeThreshold.toPx(),
                )
                horizontalDistance = 0f
            },
            onDragCancel = {
                state.cancelGesture()
                horizontalDistance = 0f
            },
        )
    }
}

private const val CatalogResultsHeaderIndex = 3
private val CatalogSectionSwipeThreshold = 56.dp

internal fun sectionSwipeDirection(horizontalDistance: Float, threshold: Float): Int? =
    if (abs(horizontalDistance) < threshold) {
        null
    } else if (horizontalDistance < 0f) {
        1
    } else {
        -1
    }

private fun BigDecimal?.asPercent(): String {
    if (this == null) return "—"
    val prefix = if (signum() > 0) "+" else ""
    return "$prefix${setScale(1, RoundingMode.HALF_UP).toPlainString()}%"
}
