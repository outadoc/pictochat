package fr.outadoc.pictochat.ui.room

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.outadoc.pictochat.domain.ChatEvent
import fr.outadoc.pictochat.domain.ProfileColor
import fr.outadoc.pictochat.domain.RoomId
import fr.outadoc.pictochat.preferences.DeviceId
import fr.outadoc.pictochat.preferences.UserProfile
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.datetime.Instant
import org.koin.compose.koinInject

@Composable
fun RoomScreen(
    modifier: Modifier = Modifier,
    roomId: RoomId,
    onBackPressed: () -> Unit,
) {
    val viewModel = koinInject<RoomViewModel>()

    BackHandler(onBack = onBackPressed)

    LaunchedEffect(roomId) {
        viewModel.onRoomSelected(roomId)
    }

    val state by viewModel.state.collectAsState()

    when (val currentState = state) {
        is RoomViewModel.State.Idle -> {
            CircularProgressIndicator()
        }

        is RoomViewModel.State.InRoom -> {
            RoomScreenContent(
                modifier = modifier,
                title = currentState.title,
                eventHistory = currentState.eventHistory,
                knownProfiles = currentState.knownProfiles,
                usersInRoom = currentState.usersInRoom,
                userProfile = currentState.userProfile,
                onBackPressed = onBackPressed,
                onSendMessage = viewModel::onSendMessage,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomScreenContent(
    modifier: Modifier = Modifier,
    title: String,
    eventHistory: ImmutableList<ChatEvent>,
    knownProfiles: ImmutableMap<DeviceId, UserProfile>,
    userProfile: UserProfile,
    usersInRoom: Int,
    onBackPressed: () -> Unit = {},
    onSendMessage: (Message) -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                },
                actions = {
                    Badge(modifier = Modifier.padding(end = 16.dp)) {
                        Text(usersInRoom.toString())
                    }
                }
            )
        }
    ) { insets ->
        Column(modifier = Modifier.padding(insets)) {
            RoomMessages(
                modifier = Modifier.weight(1f),
                eventHistory = eventHistory,
                knownProfiles = knownProfiles
            )

            RoomInput(
                modifier = Modifier
                    .imePadding()
                    .padding(16.dp),
                onSendMessage = onSendMessage,
                userProfile = userProfile
            )
        }
    }
}

@Preview
@Composable
private fun RoomScreenPreview() {
    RoomScreenContent(
        title = "Room 1",
        eventHistory = persistentListOf(
            ChatEvent.Join(
                id = "1",
                timestamp = Instant.parse("2021-09-01T12:00:00Z"),
                deviceId = DeviceId("1")
            ),
            ChatEvent.Message(
                id = "2",
                timestamp = Instant.parse("2021-09-01T12:03:00Z"),
                sender = DeviceId("1"),
                message = fr.outadoc.pictochat.domain.Message(
                    contentDescription = "Hello, world!",
                    bitmap = intArrayOf(),
                    bitmapWidth = 0,
                    bitmapHeight = 0
                )
            ),
            ChatEvent.Leave(
                id = "3",
                timestamp = Instant.parse("2021-09-01T12:05:00Z"),
                deviceId = DeviceId("1")
            )
        ),
        knownProfiles = persistentMapOf(
            DeviceId("1") to UserProfile(
                displayName = "Alice",
                displayColor = ProfileColor.Color3
            )
        ),
        usersInRoom = 42,
        userProfile = UserProfile(
            displayName = "Bob",
            displayColor = ProfileColor.Color1
        )
    )
}
