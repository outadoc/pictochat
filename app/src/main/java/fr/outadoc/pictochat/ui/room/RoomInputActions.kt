package fr.outadoc.pictochat.ui.room

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

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
