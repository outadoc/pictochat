package fr.outadoc.pictochat.ui.room

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun RoomInput(
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
