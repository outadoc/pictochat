package fr.outadoc.pictochat.ui.room

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import fr.outadoc.pictochat.ui.theme.PictoChatTextStyle
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomInput(
    modifier: Modifier = Modifier,
    onSendMessage: (Message) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val textMeasurer = rememberTextMeasurer()
    val focusRequester = remember { FocusRequester() }

    val lines = remember { mutableStateListOf<CanvasLine>() }
    var message by remember { mutableStateOf(TextFieldValue()) }

    var hasMessageContents = message.text.isNotBlank() || lines.isNotEmpty()

    val isImeVisible = WindowInsets.isImeVisible

    val bitmap =
        ImageBitmap(
            width = InputConfig.CanvasSize.width.toInt(),
            height = InputConfig.CanvasSize.height.toInt(),
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
            onValueChange = { newMessage ->
                if (newMessage.text.lines().size <= InputConfig.MaxLines) {
                    message = newMessage
                }
            }
        )

        RoomInputCanvas(
            modifier = Modifier.fillMaxWidth(),
            contentDescription = "Draw a message",
            onLineDrawn = { lines.add(it) },
            bitmap = bitmap
        )

        RoomInputActions(
            enableClearButton = hasMessageContents,
            enableSendButton = hasMessageContents,
            onToggleKeyboard = {
                if (isImeVisible) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                } else {
                    focusRequester.requestFocus()
                }
            },
            onSendMessage = {
                val pngBytes = ByteArrayOutputStream().use { buffer ->
                    val compress = bitmap
                        .asAndroidBitmap()
                        .compress(Bitmap.CompressFormat.PNG, 80, buffer)

                    if (!compress) {
                        error("Failed to compress bitmap")
                    }

                    buffer.toByteArray()
                }

                onSendMessage(
                    Message(
                        contentDescription = message.text,
                        bitmap = pngBytes
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
