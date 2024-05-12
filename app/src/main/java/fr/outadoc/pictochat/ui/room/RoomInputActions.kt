package fr.outadoc.pictochat.ui.room

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun RoomInputActions(
    modifier: Modifier = Modifier,
    enableClearButton: Boolean,
    enableSendButton: Boolean,
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

        IconButton(
            onClick = onClearMessage,
            enabled = enableClearButton
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Clear message"
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = onSendMessage,
            enabled = enableSendButton
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send message"
            )
        }
    }
}
