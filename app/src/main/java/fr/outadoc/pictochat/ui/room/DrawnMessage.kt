package fr.outadoc.pictochat.ui.room

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
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
    color: Color
) {
    Image(
        modifier = modifier
            .border(
                width = 3.dp,
                color = color,
                shape = MaterialTheme.shapes.medium
            )
            .clip(MaterialTheme.shapes.medium)
            .clipToBounds()
            .background(Color.White),
        bitmap = bitmap,
        contentScale = ContentScale.FillWidth,
        filterQuality = FilterQuality.None,
        contentDescription = contentDescription
    )
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
        bitmap = bitmap,
        contentDescription = "Preview",
        color = Color.Red
    )
}
