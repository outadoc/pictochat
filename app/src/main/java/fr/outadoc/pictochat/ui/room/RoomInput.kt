package fr.outadoc.pictochat.ui.room

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun RoomInput(
    modifier: Modifier = Modifier,
    message: TextFieldValue,
    onMessageChange: (TextFieldValue) -> Unit,
    onSendMessage: () -> Unit,
) {
    Column {
        RoomInputCanvas(modifier = modifier)
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
fun RoomInputCanvas(modifier: Modifier = Modifier) {
    val lines = remember { mutableStateListOf<Line>() }

    // Canvas size on DS: 228 x 80
    val size = Size(
        width = 228f,
        height = 80f
    )

    Canvas(
        modifier = modifier
            .height(size.height.dp)
            .width(size.width.dp)
            .border(1.dp, MaterialTheme.colorScheme.onPrimary)
            .clipToBounds()
            .background(Color.White)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()

                    lines.add(
                        Line(
                            // Determines the starting position of the line
                            start = change.position - dragAmount,
                            end = change.position
                        )
                    )
                }
            },
        onDraw = {
            lines.forEach { line ->
                drawLine(
                    color = Color.Black,
                    start = line.start,
                    end = line.end,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    )
}
