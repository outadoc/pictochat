package fr.outadoc.pictochat.ui.room

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import fr.outadoc.pictochat.domain.ChatEvent
import fr.outadoc.pictochat.preferences.DeviceId
import fr.outadoc.pictochat.preferences.UserProfile
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap


@Composable
fun RoomMessages(
    modifier: Modifier = Modifier,
    eventHistory: ImmutableList<ChatEvent>,
    knownProfiles: ImmutableMap<DeviceId, UserProfile>,
) {
    LazyColumn(
        modifier = modifier
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
