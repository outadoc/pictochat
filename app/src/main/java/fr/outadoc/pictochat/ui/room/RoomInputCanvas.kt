package fr.outadoc.pictochat.ui.room

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp

@Composable
fun RoomInputCanvas(
    modifier: Modifier = Modifier,
    contentDescription: String,
    onLineDrawn: (CanvasLine) -> Unit,
    bitmap: ImageBitmap,
) {
    var canvasWidthPx: Float? by remember { mutableStateOf(null) }

    Box(
        modifier = modifier
            .aspectRatio(InputConfig.CanvasRatio)
    ) {
        DrawnMessage(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { imageSize ->
                    canvasWidthPx = imageSize.width.toFloat()
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        val width = canvasWidthPx ?: return@detectDragGestures

                        change.consume()

                        val imageDensity = bitmap.width / width

                        onLineDrawn(
                            CanvasLine(
                                // Determines the starting position of the line
                                start = (change.position - dragAmount) * imageDensity,
                                end = change.position * imageDensity
                            )
                        )
                    }
                },
            bitmap = bitmap,
            contentDescription = contentDescription
        )

        CanvasGuidelines(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        )
    }
}
