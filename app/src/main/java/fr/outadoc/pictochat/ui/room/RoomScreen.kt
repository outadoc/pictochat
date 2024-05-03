package fr.outadoc.pictochat.ui.room

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.outadoc.pictochat.domain.ChatEvent
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
                currentMessage = currentState.currentMessage,
                onBackPressed = onBackPressed,
                onMessageChanged = viewModel::onMessageChanged,
                onSendMessage = viewModel::onSendMessage,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoomScreenContent(
    modifier: Modifier = Modifier,
    title: String,
    eventHistory: ImmutableList<ChatEvent>,
    knownProfiles: ImmutableMap<DeviceId, UserProfile>,
    currentMessage: String,
    onBackPressed: () -> Unit = {},
    onMessageChanged: (String) -> Unit = {},
    onSendMessage: () -> Unit = {},
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
                }
            )
        }
    ) { insets ->
        Column(modifier = Modifier.padding(insets)) {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(
                    eventHistory,
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
                            val profile = knownProfiles.getProfile(event.deviceId)
                            ListItem(
                                headlineContent = {
                                    Text(
                                        "${profile.displayName} joined",
                                        fontStyle = FontStyle.Italic
                                    )
                                },
                                overlineContent = { Text(event.timestamp.toString()) }
                            )
                        }

                        is ChatEvent.Leave -> {
                            val profile = knownProfiles.getProfile(event.deviceId)
                            ListItem(
                                headlineContent = {
                                    Text(
                                        "${profile.displayName} left",
                                        fontStyle = FontStyle.Italic
                                    )
                                },
                                overlineContent = { Text(event.timestamp.toString()) }
                            )
                        }

                        is ChatEvent.TextMessage -> {
                            val profile = knownProfiles.getProfile(event.sender)
                            ListItem(
                                headlineContent = {
                                    Text(
                                        buildAnnotatedString {
                                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                                append(profile.displayName)
                                                append(": ")
                                            }
                                            append(event.message)
                                        }
                                    )
                                },
                                overlineContent = { Text(event.timestamp.toString()) }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .imePadding()
                    .padding(16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                TextField(
                    modifier = Modifier.weight(1f),
                    value = currentMessage,
                    onValueChange = onMessageChanged
                )

                IconButton(onClick = onSendMessage) {
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
    RoomScreenContent(
        title = "Room 1",
        currentMessage = "Hello, world!",
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
        ),
        knownProfiles = persistentMapOf(
            DeviceId("1") to UserProfile(
                displayName = "Alice",
                displayColor = 0xFF0000
            )
        )
    )
}

@Composable
private fun Map<DeviceId, UserProfile>.getProfile(deviceId: DeviceId): UserProfile {
    return remember(this, deviceId) {
        get(deviceId) ?: UserProfile(
            displayName = deviceId.value,
            displayColor = 0xFF0000
        )
    }
}
