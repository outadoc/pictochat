package fr.outadoc.pictochat.ui.room

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection

@Composable
fun RoomInput(
    modifier: Modifier = Modifier,
    onSendMessage: (Message) -> Unit,
) {
    val lines = remember { mutableStateListOf<Line>() }
    var message: TextFieldValue by remember { mutableStateOf(TextFieldValue()) }

    val bitmap =
        ImageBitmap(
            width = InputConfig.CanvasSize.width.toInt(),
            height = InputConfig.CanvasSize.height.toInt(),
            config = ImageBitmapConfig.Alpha8
        )

    val drawScope = CanvasDrawScope()
    val canvas = Canvas(bitmap)

    drawScope.draw(
        density = LocalDensity.current,
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = InputConfig.CanvasSize,
    ) {
        lines.forEach { line ->
            drawLine(
                color = Color.Black,
                start = line.start,
                end = line.end,
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
    }

    Column {
        RoomInputCanvas(
            modifier = modifier,
            contentDescription = "Draw a message",
            onLineDrawn = { lines.add(it) },
            bitmap = bitmap
        )

        RoomInputField(
            modifier = modifier,
            message = message,
            onMessageChange = { message = it },
            onSendMessage = {
                val pixelMap = bitmap.toPixelMap()
                onSendMessage(
                    Message(
                        message = message.text,
                        bitmap = pixelMap.buffer,
                        bitmapWidth = pixelMap.width,
                        bitmapHeight = pixelMap.height
                    )
                )
                message = TextFieldValue()
                lines.clear()
            }
        )
    }
}

@Composable
fun RoomInputField(
    modifier: Modifier = Modifier,
    message: TextFieldValue,
    onMessageChange: (TextFieldValue) -> Unit,
    onSendMessage: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        TextField(
            modifier = Modifier.weight(1f),
            value = message,
            onValueChange = { onMessageChange(it) },
        )

        IconButton(onClick = onSendMessage) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send message"
            )
        }
    }
}

@Composable
private fun RoomInputCanvas(
    modifier: Modifier = Modifier,
    contentDescription: String,
    onLineDrawn: (Line) -> Unit,
    bitmap: ImageBitmap,
) {
    var canvasWidthPx: Float? by remember { mutableStateOf(null) }

    DrawnMessage(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { imageSize ->
                canvasWidthPx = imageSize.width.toFloat()
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    val width = canvasWidthPx ?: return@detectDragGestures

                    change.consume()

                    val imageDensity = bitmap.width / width

                    onLineDrawn(
                        Line(
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
}

private data class Line(
    val start: Offset,
    val end: Offset,
)
