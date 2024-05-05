package fr.outadoc.pictochat.ui.room

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun RoomInput(
    modifier: Modifier = Modifier,
    message: TextFieldValue,
    onMessageChange: (TextFieldValue) -> Unit,
    onSendMessage: () -> Unit,
) {
    Column {
        RoomInputCanvas(
            modifier = modifier,
            contentDescription = "Draw a message"
        )

        RoomInputField(
            modifier = modifier,
            message = message,
            onMessageChange = onMessageChange,
            onSendMessage = onSendMessage
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
fun RoomInputCanvas(
    modifier: Modifier = Modifier,
    contentDescription: String,
) {
    // Canvas size on DS: 228 x 80
    val size = Size(
        width = 228f,
        height = 80f
    )

    val bitmap =
        ImageBitmap(
            width = size.width.toInt(),
            height = size.height.toInt(),
            config = ImageBitmapConfig.Alpha8
        )

    val drawScope = CanvasDrawScope()
    val canvas = Canvas(bitmap)

    val lines = remember { mutableStateListOf<Line>() }

    drawScope.draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = size,
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

    var canvasWidthPx: Float? by remember { mutableStateOf(null) }

    Image(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onPrimary)
            .clipToBounds()
            .background(Color.White)
            .onSizeChanged { imageSize ->
                canvasWidthPx = imageSize.width.toFloat()
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    val width = canvasWidthPx ?: return@detectDragGestures

                    change.consume()

                    val imageDensity = bitmap.width / width

                    lines.add(
                        Line(
                            // Determines the starting position of the line
                            start = (change.position - dragAmount) * imageDensity,
                            end = change.position * imageDensity
                        )
                    )
                }
            },
        bitmap = bitmap,
        contentScale = ContentScale.FillWidth,
        contentDescription = contentDescription
    )
}
