package tw.taipei.veges.designsystem

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/** Material 3 emphasized easing, used for container transforms and page transitions. */
val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
const val ContainerTransformDurationMillis = 400

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

private data class ProduceSharedKey(val conceptId: String, val part: String)

/**
 * Morphs a produce row into the produce detail page. Both sides must use the same [conceptId].
 * Outside a shared transition (tests, previews) this is a no-op.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.produceContainerTransform(conceptId: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val visibilityScope = LocalNavAnimatedVisibilityScope.current ?: return this
    return with(sharedScope) {
        this@produceContainerTransform.sharedBounds(
            sharedContentState = rememberSharedContentState(ProduceSharedKey(conceptId, "container")),
            animatedVisibilityScope = visibilityScope,
            enter = fadeIn(tween(durationMillis = 250, delayMillis = 100, easing = EmphasizedDecelerateEasing)),
            exit = fadeOut(tween(durationMillis = 100, easing = EmphasizedAccelerateEasing)),
            boundsTransform = { _, _ -> tween(ContainerTransformDurationMillis, easing = EmphasizedEasing) },
            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopCenter,
            ),
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.produceSharedIllustration(
    conceptId: String,
    shape: Shape = RoundedCornerShape(16.dp),
): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val visibilityScope = LocalNavAnimatedVisibilityScope.current ?: return this
    return with(sharedScope) {
        this@produceSharedIllustration.sharedElement(
            sharedContentState = rememberSharedContentState(ProduceSharedKey(conceptId, "illustration")),
            animatedVisibilityScope = visibilityScope,
            boundsTransform = { _, _ -> tween(ContainerTransformDurationMillis, easing = EmphasizedEasing) },
            clipInOverlayDuringTransition = OverlayClip(shape),
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.produceSharedName(conceptId: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val visibilityScope = LocalNavAnimatedVisibilityScope.current ?: return this
    return with(sharedScope) {
        this@produceSharedName.sharedBounds(
            sharedContentState = rememberSharedContentState(ProduceSharedKey(conceptId, "name")),
            animatedVisibilityScope = visibilityScope,
            boundsTransform = { _, _ -> tween(ContainerTransformDurationMillis, easing = EmphasizedEasing) },
            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
            ),
        )
    }
}
