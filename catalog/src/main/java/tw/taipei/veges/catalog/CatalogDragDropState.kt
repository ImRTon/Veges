package tw.taipei.veges.catalog

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val CatalogItemKeyPrefix = "catalog-item:"

internal fun catalogItemKey(conceptId: String): String = "$CatalogItemKeyPrefix$conceptId"

private fun Any.conceptIdOrNull(): String? =
    (this as? String)
        ?.takeIf { it.startsWith(CatalogItemKeyPrefix) }
        ?.removePrefix(CatalogItemKeyPrefix)

internal class CatalogDragDropState(
    private val lazyListState: LazyListState,
    private val coroutineScope: CoroutineScope,
    private val onMove: (draggedConceptId: String, targetConceptId: String) -> Unit,
) {
    private var draggedItemKey by mutableStateOf<Any?>(null)
    private var initialItemOffset by mutableFloatStateOf(0f)
    private var draggedDistance by mutableFloatStateOf(0f)
    private var draggedItemSize = 0
    private var lastKnownDraggedItemOffset by mutableFloatStateOf(0f)
    private var lastTargetConceptId: String? = null
    private var lastMoveDirection: Int? = null
    private var autoScrollBy = 0f
    private var scrollJob: Job? = null

    val isDragging: Boolean
        get() = draggedItemKey != null

    val draggedConceptId: String?
        get() = draggedItemKey?.conceptIdOrNull()

    val draggedItemOffset: Float
        get() = draggedItemInfo
            ?.let { initialItemOffset + draggedDistance - it.offset }
            ?: lastKnownDraggedItemOffset

    private val draggedItemInfo: LazyListItemInfo?
        get() = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key == draggedItemKey }

    fun onDragStart(pointerY: Float): Boolean {
        val item = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
            it.key.conceptIdOrNull() != null &&
                pointerY.toInt() in it.offset..(it.offset + it.size)
        } ?: return false
        draggedItemKey = item.key
        initialItemOffset = item.offset.toFloat()
        draggedDistance = 0f
        draggedItemSize = item.size
        lastKnownDraggedItemOffset = 0f
        lastTargetConceptId = null
        lastMoveDirection = null
        return true
    }

    fun onDrag(deltaY: Float) {
        draggedDistance += deltaY
        rememberDraggedItemOffset()
        moveToItemUnderDraggedCenter()

        val viewportStart = lazyListState.layoutInfo.viewportStartOffset.toFloat()
        val viewportEnd = lazyListState.layoutInfo.viewportEndOffset.toFloat()
        val draggedTop = initialItemOffset + draggedDistance
        val draggedBottom = draggedTop + draggedItemSize
        val scrollBy = when {
            draggedTop < viewportStart -> draggedTop - viewportStart
            draggedBottom > viewportEnd -> draggedBottom - viewportEnd
            else -> 0f
        }.coerceIn(-18f, 18f)
        updateAutoScroll(scrollBy)
    }

    private fun moveToItemUnderDraggedCenter() {
        val draggedItem = draggedItemInfo ?: return
        rememberDraggedItemOffset()
        val draggedCenter = initialItemOffset + draggedDistance + draggedItemSize / 2f
        val targetItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { candidate ->
            candidate.key != draggedItem.key &&
                candidate.key.conceptIdOrNull() != null &&
                draggedCenter >= candidate.offset &&
                draggedCenter <= candidate.offset + candidate.size
        }
        val draggedId = draggedItem.key.conceptIdOrNull()
        val targetId = targetItem?.key?.conceptIdOrNull()
        val moveDirection = targetItem?.index?.compareTo(draggedItem.index)
        if (
            draggedId != null &&
            targetId != null &&
            (targetId != lastTargetConceptId || moveDirection != lastMoveDirection)
        ) {
            lastTargetConceptId = targetId
            lastMoveDirection = moveDirection
            onMove(draggedId, targetId)
        }
    }

    private fun rememberDraggedItemOffset() {
        draggedItemInfo?.let { item ->
            lastKnownDraggedItemOffset = initialItemOffset + draggedDistance - item.offset
        }
    }

    private fun updateAutoScroll(scrollBy: Float) {
        autoScrollBy = scrollBy
        if (scrollBy == 0f) {
            scrollJob?.cancel()
            scrollJob = null
        } else if (scrollJob?.isActive != true) {
            scrollJob = coroutineScope.launch {
                while (isActive && autoScrollBy != 0f) {
                    val consumed = lazyListState.scrollBy(autoScrollBy)
                    if (draggedItemInfo == null) {
                        lastKnownDraggedItemOffset += consumed
                    } else {
                        rememberDraggedItemOffset()
                    }
                    moveToItemUnderDraggedCenter()
                    if (consumed == 0f) break
                    delay(16)
                }
            }
        }
    }

    fun onDragEnd() {
        autoScrollBy = 0f
        scrollJob?.cancel()
        scrollJob = null
        draggedItemKey = null
        initialItemOffset = 0f
        draggedDistance = 0f
        draggedItemSize = 0
        lastKnownDraggedItemOffset = 0f
        lastTargetConceptId = null
        lastMoveDirection = null
    }
}
