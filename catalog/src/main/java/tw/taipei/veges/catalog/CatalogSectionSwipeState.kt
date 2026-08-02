package tw.taipei.veges.catalog

import androidx.compose.foundation.MutatePriority
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.carousel.CarouselState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
internal class CatalogSectionSwipeState(
    private val carouselState: CarouselState,
    private val coroutineScope: CoroutineScope,
    private val sectionCount: Int,
) {
    var selectedSectionIndex by mutableIntStateOf(carouselState.currentItem.validIndex())
        private set

    private var gestureStartIndex = selectedSectionIndex
    private var gestureActive = false
    private var settling = false
    private var dragChannel: Channel<Float>? = null
    private var dragJob: Job? = null
    private var settleJob: Job? = null

    fun syncFromCarousel() {
        if (!gestureActive && !settling) {
            selectedSectionIndex = carouselState.currentItem.validIndex()
        }
    }

    fun startGesture() {
        settleJob?.cancel()
        settleJob = null
        dragChannel?.close()
        dragJob?.cancel()
        settling = false
        selectedSectionIndex = carouselState.currentItem.validIndex()
        gestureStartIndex = selectedSectionIndex
        gestureActive = true
        val channel = Channel<Float>(Channel.UNLIMITED)
        dragChannel = channel
        dragJob = coroutineScope.launch {
            carouselState.scroll(MutatePriority.UserInput) {
                for (pointerDeltaX in channel) {
                    scrollBy(-pointerDeltaX)
                }
            }
        }
    }

    fun dragBy(pointerDeltaX: Float) {
        if (gestureActive) {
            dragChannel?.trySend(pointerDeltaX)
        }
    }

    fun finishGesture(horizontalDistance: Float, threshold: Float) {
        if (!gestureActive) return
        val direction = sectionSwipeDirection(horizontalDistance, threshold) ?: 0
        settleTo((gestureStartIndex + direction).validIndex())
    }

    fun cancelGesture() {
        if (gestureActive) settleTo(gestureStartIndex)
    }

    fun animateBy(direction: Int) {
        settleJob?.cancel()
        gestureActive = false
        settleTo((selectedSectionIndex + direction).validIndex())
    }

    private fun settleTo(targetIndex: Int) {
        gestureActive = false
        settling = true
        selectedSectionIndex = targetIndex
        dragChannel?.close()
        dragChannel = null
        val activeDragJob = dragJob
        dragJob = null
        settleJob = coroutineScope.launch {
            activeDragJob?.join()
            carouselState.animateScrollToItem(targetIndex)
            carouselState.scrollToItem(targetIndex)
            settling = false
            selectedSectionIndex = carouselState.currentItem.validIndex()
        }
    }

    private fun Int.validIndex(): Int = coerceIn(0, (sectionCount - 1).coerceAtLeast(0))
}
