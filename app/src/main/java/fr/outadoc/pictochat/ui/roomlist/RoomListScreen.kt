package fr.outadoc.pictochat.ui.roomlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.outadoc.pictochat.domain.RoomId
import fr.outadoc.pictochat.domain.RoomState
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(
    modifier: Modifier = Modifier,
    nearbyUserCount: Int,
    roomStates: ImmutableList<RoomState>,
    onRoomSelected: (RoomId) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("PictoChat") },
            )
        },
        bottomBar = {
            BottomAppBar(
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("$nearbyUserCount nearby users")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier.padding(innerPadding)
        ) {
            items(roomStates) { room ->
                ListItem(
                    modifier = Modifier.clickable { onRoomSelected(room.id) },
                    headlineContent = { Text(room.displayName) },
                    supportingContent = {
                        Text("${room.connectedDevices.size} participants")
                    }
                )
            }
        }
    }
}
