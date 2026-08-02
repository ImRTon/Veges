package tw.taipei.veges.designsystem

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

@Composable
fun ProduceIllustration(
    assetPath: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current
    val bitmap = remember(context, assetPath) {
        assetPath?.let { path ->
            runCatching {
                context.assets.open(path).use(BitmapFactory::decodeStream)?.asImageBitmap()
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            val leafColor = MaterialTheme.colorScheme.primary
            Canvas(Modifier.fillMaxSize()) {
                val stroke = size.minDimension * 0.07f
                drawLine(
                    color = leafColor,
                    start = Offset(size.width * 0.50f, size.height * 0.78f),
                    end = Offset(size.width * 0.50f, size.height * 0.30f),
                    strokeWidth = stroke,
                )
                drawOval(
                    color = leafColor,
                    topLeft = Offset(size.width * 0.18f, size.height * 0.22f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.34f, size.height * 0.36f),
                )
                drawOval(
                    color = leafColor,
                    topLeft = Offset(size.width * 0.48f, size.height * 0.30f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.34f, size.height * 0.36f),
                )
            }
        }
    }
}
