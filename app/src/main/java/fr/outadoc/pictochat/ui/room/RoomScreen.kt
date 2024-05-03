package fr.outadoc.pictochat.ui.room

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.outadoc.pictochat.domain.ChatEvent
import fr.outadoc.pictochat.domain.RoomId
import fr.outadoc.pictochat.domain.RoomState
import fr.outadoc.pictochat.preferences.DeviceId
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(
    modifier: Modifier = Modifier,
    roomState: RoomState,
    onSendMessage: (String) -> Unit,
    onBackPressed: () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(roomState.displayName)
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                }
            )
        }
    ) { insets ->
        BackHandler(onBack = onBackPressed)
        Column(modifier = Modifier.padding(insets)) {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(
                    roomState.eventHistory,
                    contentType = { event ->
                        when (event) {
                            is ChatEvent.Join -> 1
                            is ChatEvent.Leave -> 2
                            is ChatEvent.TextMessage -> 3
                        }
                    },
                    key = { event -> event.id }
                ) { event ->
                    when (event) {
                        is ChatEvent.Join -> {
                            ListItem(
                                headlineContent = { Text(">>> ${event.deviceId} joined") }
                            )
                        }

                        is ChatEvent.Leave -> {
                            ListItem(
                                headlineContent = { Text(">>> ${event.deviceId} left") }
                            )
                        }

                        is ChatEvent.TextMessage -> {
                            ListItem(
                                overlineContent = { Text("${event.sender}") },
                                headlineContent = { Text(event.message) }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                TextField(
                    modifier = Modifier.weight(1f),
                    value = "",
                    onValueChange = {}
                )
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message"
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun RoomScreenPreview() {
    RoomScreen(
        roomState = RoomState(
            id = RoomId(0),
            displayName = "Room 1",
            eventHistory = persistentListOf(
                ChatEvent.Join(
                    id = "1",
                    timestamp = Instant.parse("2021-09-01T12:00:00Z"),
                    deviceId = DeviceId("1")
                ),
                ChatEvent.TextMessage(
                    id = "2",
                    timestamp = Instant.parse("2021-09-01T12:03:00Z"),
                    sender = DeviceId("1"),
                    message = "Hello, world!"
                ),
                ChatEvent.Leave(
                    id = "3",
                    timestamp = Instant.parse("2021-09-01T12:05:00Z"),
                    deviceId = DeviceId("1")
                )
            )
        ),
        onSendMessage = {},
        onBackPressed = {}
    )
}