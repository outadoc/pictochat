package fr.outadoc.pictochat.ui.room

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RoomInputCanvas(
            modifier = Modifier.fillMaxWidth(),
            contentDescription = "Draw a message",
            onLineDrawn = { lines.add(it) },
            bitmap = bitmap
        )

        RoomInputActions(
            onToggleKeyboard = {

            },
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
            },
            onClearMessage = {
                message = TextFieldValue()
                lines.clear()
            }
        )
    }
}

@Composable
fun RoomInputActions(
    modifier: Modifier = Modifier,
    onToggleKeyboard: () -> Unit,
    onClearMessage: () -> Unit,
    onSendMessage: () -> Unit,
) {
    Row(modifier = modifier) {
        IconButton(onClick = onToggleKeyboard) {
            Icon(
                Icons.Default.Keyboard,
                contentDescription = "Toggle keyboard"
            )
        }

        IconButton(onClick = onClearMessage) {
            Icon(
                Icons.Default.Clear,
                contentDescription = "Clear message"
            )
        }

        Spacer(modifier = Modifier.weight(1f))

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
