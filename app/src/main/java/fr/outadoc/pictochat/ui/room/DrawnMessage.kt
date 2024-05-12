package fr.outadoc.pictochat.ui.room

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun DrawnMessage(
    modifier: Modifier = Modifier,
    bitmap: ImageBitmap,
    contentDescription: String,
) {
    Box(
        modifier
            .border(
                width = 3.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium
            )
    ) {
        Image(
            modifier = Modifier
                .padding(3.dp)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clipToBounds()
                .background(MaterialTheme.colorScheme.secondaryContainer),
            bitmap = bitmap,
            contentScale = ContentScale.FillWidth,
            filterQuality = FilterQuality.None,
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(
                MaterialTheme.colorScheme.onSecondaryContainer
            )
        )
    }
}

@Preview
@Composable
private fun DrawnMessagePreview() {
    val bitmap = ImageBitmap(
        width = InputConfig.CanvasSize.width.toInt(),
        height = InputConfig.CanvasSize.height.toInt(),
    )

    val canvas = Canvas(bitmap)

    canvas.drawCircle(
        center = Offset(
            x = InputConfig.CanvasSize.width / 2,
            y = InputConfig.CanvasSize.height / 2
        ),
        radius = 30f,
        paint = Paint().apply {
            color = Color.Black
        }
    )

    DrawnMessage(
        modifier = Modifier.width(512.dp),
        bitmap = bitmap,
        contentDescription = "Preview"
    )
}
