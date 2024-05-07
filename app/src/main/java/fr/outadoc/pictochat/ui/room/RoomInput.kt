package fr.outadoc.pictochat.ui.room

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import fr.outadoc.pictochat.preferences.UserProfile
import fr.outadoc.pictochat.ui.theme.PictoChatTextStyle
import fr.outadoc.pictochat.ui.theme.toColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomInput(
    modifier: Modifier = Modifier,
    onSendMessage: (Message) -> Unit,
    userProfile: UserProfile
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val textMeasurer = rememberTextMeasurer()
    val focusRequester = remember { FocusRequester() }

    val lines = remember { mutableStateListOf<Line>() }
    var message by remember { mutableStateOf(TextFieldValue()) }

    val isImeVisible = WindowInsets.isImeVisible

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
        val horizontalPaddingPx = 4f
        val verticalPaddingPx = 5f

        drawText(
            textMeasurer,
            text = message.text,
            style = PictoChatTextStyle,
            topLeft = Offset(horizontalPaddingPx, verticalPaddingPx),
            size = Size(
                width = InputConfig.CanvasSize.width - 2 * horizontalPaddingPx,
                height = InputConfig.CanvasSize.height - 2 * verticalPaddingPx
            )
        )

        lines.forEach { line ->
            drawLine(
                color = Color.Black,
                start = line.start,
                end = line.end,
                strokeWidth = 2f,
                cap = StrokeCap.Square
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RoomInputTextField(
            modifier = Modifier
                .focusRequester(focusRequester),
            value = message,
            onValueChange = { message = it }
        )

        RoomInputCanvas(
            modifier = Modifier.fillMaxWidth(),
            contentDescription = "Draw a message",
            onLineDrawn = { lines.add(it) },
            bitmap = bitmap,
            favoriteColor = userProfile.displayColor.toColor()
        )

        RoomInputActions(
            onToggleKeyboard = {
                if (isImeVisible) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                } else {
                    focusRequester.requestFocus()
                }
            },
            onSendMessage = {
                val pixelMap = bitmap.toPixelMap()
                onSendMessage(
                    Message(
                        contentDescription = message.text,
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
                Icons.Default.Delete,
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
private fun RoomInputTextField(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
) {
    BasicTextField(
        modifier = modifier.size(1.dp),
        value = value,
        onValueChange = onValueChange
    )
}

@Composable
private fun RoomInputCanvas(
    modifier: Modifier = Modifier,
    contentDescription: String,
    onLineDrawn: (Line) -> Unit,
    bitmap: ImageBitmap,
    favoriteColor: Color
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
        contentDescription = contentDescription,
        color = favoriteColor
    )
}

private data class Line(
    val start: Offset,
    val end: Offset,
)
