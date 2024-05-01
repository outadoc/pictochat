package fr.outadoc.pictochat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.outadoc.pictochat.domain.Room

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(
    modifier: Modifier = Modifier,
    room: Room,
    onSendMessage: (String) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(room.displayName)
                }
            )
        }
    ) { insets ->
        Column(modifier = Modifier.padding(insets)) {
            TextField(value = "", onValueChange = {})
            IconButton(onClick = { /*TODO*/ }) {
                Icon(Icons.Default.Send, contentDescription = "Send message")
            }
        }
    }
}
