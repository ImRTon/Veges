package tw.taipei.veges.home

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.imageResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val MESH_SIZE = 36
private const val EYE_RADIUS = 0.12f
private const val SHEAR_CYCLE_MILLIS = 3_200
private const val MAX_SHEAR_RADIANS = (80.0 * PI / 180.0).toFloat()
private const val TAU = (PI * 2.0).toFloat()

/**
 * Spins the storm with differential rotation: the eyewall turns faster than the outer rain
 * bands, like a real cyclone. The extra inner twist is applied in two staggered phases that
 * cross-fade, so the spiral keeps its shape instead of winding up over time.
 *
 * [rotationProgress] drives one counterclockwise turn of the outer rim per cycle.
 */
@Composable
internal fun TyphoonVortex(
    rotationProgress: Float,
    modifier: Modifier = Modifier,
) {
    val image = ImageBitmap.imageResource(R.drawable.typhoon_material)
    val mesh = remember(image) { TyphoonMesh(image) }
    val shearProgress by rememberInfiniteTransition(label = "typhoon-shear").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHEAR_CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "typhoon-shear-phase",
    )
    Canvas(modifier = modifier) {
        mesh.draw(
            scope = this,
            rotation = -rotationProgress * TAU,
            shearPhase = shearProgress,
        )
    }
}

private class TyphoonMesh(image: ImageBitmap) {
    private val bitmap = image.asAndroidBitmap()
    private val vertexCount = (MESH_SIZE + 1) * (MESH_SIZE + 1)

    // Polar coordinates of each mesh vertex relative to the bitmap centre, radius normalised to 1.
    private val radius = FloatArray(vertexCount)
    private val angle = FloatArray(vertexCount)
    private val shearWeight = FloatArray(vertexCount)

    private val leadingVertices = FloatArray(vertexCount * 2)
    private val trailingVertices = FloatArray(vertexCount * 2)

    private val leadingPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val trailingPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        // Additive blend keeps the cross-fade energy-preserving on premultiplied colours.
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }

    init {
        for (row in 0..MESH_SIZE) {
            for (column in 0..MESH_SIZE) {
                val index = row * (MESH_SIZE + 1) + column
                val x = column.toFloat() / MESH_SIZE * 2f - 1f
                val y = row.toFloat() / MESH_SIZE * 2f - 1f
                val r = sqrt(x * x + y * y)
                radius[index] = r
                angle[index] = atan2(y, x)
                val outward = ((r - EYE_RADIUS) / (1f - EYE_RADIUS)).coerceIn(0f, 1f)
                shearWeight[index] = (1f - outward).pow(1.6f)
            }
        }
    }

    fun draw(scope: DrawScope, rotation: Float, shearPhase: Float) {
        val side = scope.size.minDimension
        if (side <= 0f) return
        val halfSide = side / 2f
        val centerX = scope.size.width / 2f
        val centerY = scope.size.height / 2f

        val leadingPhase = shearPhase
        val trailingPhase = (shearPhase + 0.5f) % 1f
        fillVertices(leadingVertices, rotation, leadingPhase, centerX, centerY, halfSide)
        fillVertices(trailingVertices, rotation, trailingPhase, centerX, centerY, halfSide)
        leadingPaint.alpha = (phaseWeight(leadingPhase) * 255f).toInt()
        trailingPaint.alpha = 255 - leadingPaint.alpha

        scope.drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val layer = native.saveLayer(0f, 0f, scope.size.width, scope.size.height, null)
            native.drawBitmapMesh(
                bitmap, MESH_SIZE, MESH_SIZE, leadingVertices, 0, null, 0, leadingPaint,
            )
            native.drawBitmapMesh(
                bitmap, MESH_SIZE, MESH_SIZE, trailingVertices, 0, null, 0, trailingPaint,
            )
            native.restoreToCount(layer)
        }
    }

    private fun fillVertices(
        target: FloatArray,
        rotation: Float,
        phase: Float,
        centerX: Float,
        centerY: Float,
        halfSide: Float,
    ) {
        val shear = (phase - 0.5f) * MAX_SHEAR_RADIANS
        for (index in 0 until vertexCount) {
            val theta = angle[index] + rotation - shear * shearWeight[index]
            val distance = radius[index] * halfSide
            target[index * 2] = centerX + cos(theta) * distance
            target[index * 2 + 1] = centerY + sin(theta) * distance
        }
    }

    private fun phaseWeight(phase: Float): Float = 1f - abs(2f * phase - 1f)
}
