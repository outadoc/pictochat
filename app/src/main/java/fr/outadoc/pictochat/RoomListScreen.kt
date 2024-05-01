package fr.outadoc.pictochat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import fr.outadoc.pictochat.domain.Room

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(
    modifier: Modifier = Modifier,
    rooms: List<Room>,
    onRoomSelected: (Room) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Pictochat") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier.padding(innerPadding)
        ) {
            items(rooms) { room ->
                val roomState by room.getRoomState().collectAsState(Room.State.NotConnected())

                ListItem(
                    modifier = Modifier.clickable { onRoomSelected(room) },
                    headlineContent = { Text(room.displayName) },
                    supportingContent = { Text("Connected clients: ${roomState.connectedClientCount}") },
                )
            }
        }
    }
}
