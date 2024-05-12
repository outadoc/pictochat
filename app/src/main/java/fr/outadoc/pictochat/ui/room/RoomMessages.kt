package fr.outadoc.pictochat.ui.room

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import fr.outadoc.pictochat.domain.ChatEvent
import fr.outadoc.pictochat.domain.ProfileColor
import fr.outadoc.pictochat.preferences.DeviceId
import fr.outadoc.pictochat.preferences.UserProfile
import fr.outadoc.pictochat.ui.theme.toColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf


@Composable
fun RoomMessages(
    modifier: Modifier = Modifier,
    eventHistory: ImmutableList<ChatEvent>,
    knownProfiles: ImmutableMap<DeviceId, UserProfile>,
) {
    var isListAtBottom by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    LaunchedEffect(eventHistory) {
        if (isListAtBottom) {
            listState.scrollToItem(
                index = (eventHistory.size - 1).coerceAtLeast(0),
            )
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
    ) {
        items(
            eventHistory,
            contentType = { event ->
                when (event) {
                    is ChatEvent.Join -> 1
                    is ChatEvent.Leave -> 2
                    is ChatEvent.Message -> 3
                }
            },
            key = { event -> event.id }
        ) { event ->
            ChatEvent(
                event = event,
                knownProfiles = knownProfiles
            )
        }

        item(key = "bottom") {
            LaunchedEffect(Unit) {
                isListAtBottom = true
            }

            DisposableEffect(Unit) {
                onDispose {
                    isListAtBottom = false
                }
            }
        }
    }
}

@Composable
private fun ChatEvent(
    modifier: Modifier = Modifier,
    event: ChatEvent,
    knownProfiles: ImmutableMap<DeviceId, UserProfile> = persistentMapOf(),
) {
    when (event) {
        is ChatEvent.Join -> {
            val profile = knownProfiles.getProfile(event.deviceId)
            ListItem(
                modifier = modifier,
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
                modifier = modifier,
                headlineContent = {
                    Text(
                        "${profile.displayName} left",
                        fontStyle = FontStyle.Italic
                    )
                },
                overlineContent = { Text(event.timestamp.toString()) }
            )
        }

        is ChatEvent.Message -> {
            val profile = knownProfiles.getProfile(event.sender)
            ListItem(
                modifier = modifier,
                overlineContent = { Text(event.timestamp.toString()) },
                headlineContent = {
                    Column {
                        Text(
                            buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(profile.displayName)
                                }
                            }
                        )

                        val bitmap = remember(event.message) {
                            BitmapFactory
                                .decodeByteArray(
                                    event.message.drawing,
                                    0,
                                    event.message.drawing.size
                                )
                                .asImageBitmap()
                        }

                        DrawnMessage(
                            modifier = Modifier.fillMaxWidth(),
                            bitmap = bitmap,
                            color = profile.displayColor.toColor(),
                            contentDescription = buildString {
                                append("Canvas sent by ")
                                append(profile.displayName)
                                append(".")

                                if (event.message.contentDescription.isNotBlank()) {
                                    append("Drawing description: ")
                                    append(event.message.contentDescription)
                                }
                            }
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun Map<DeviceId, UserProfile>.getProfile(deviceId: DeviceId): UserProfile {
    return remember(this, deviceId) {
        get(deviceId) ?: UserProfile(
            displayName = "Unknown User",
            displayColor = ProfileColor.Default
        )
    }
}
