package fr.outadoc.pictochat.ui.room

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.core.graphics.createBitmap
import fr.outadoc.pictochat.domain.ChatEvent
import fr.outadoc.pictochat.domain.ProfileColor
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
                    is ChatEvent.Message -> 3
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

                is ChatEvent.Message -> {
                    val profile = knownProfiles.getProfile(event.sender)
                    ListItem(
                        overlineContent = { Text(event.timestamp.toString()) },
                        headlineContent = {
                            Column {
                                Text(
                                    buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append(profile.displayName)
                                            append(": ")
                                        }
                                        append(event.message.text)
                                    }
                                )

                                val bitmap = remember(event.message) {
                                    createBitmap(
                                        width = event.message.bitmapWidth,
                                        height = event.message.bitmapHeight,
                                        config = Bitmap.Config.ALPHA_8,
                                    )
                                        .apply {
                                            setPixels(
                                                /* pixels = */ event.message.bitmap,
                                                /* offset = */ 0,
                                                /* stride = */ event.message.bitmapWidth,
                                                /* x = */ 0,
                                                /* y = */ 0,
                                                /* width = */ event.message.bitmapWidth,
                                                /* height = */ event.message.bitmapHeight
                                            )
                                        }
                                        .asImageBitmap()
                                }

                                DrawnMessage(
                                    modifier = Modifier.fillMaxWidth(),
                                    bitmap = bitmap,
                                    contentDescription = "Canvas sent by ${profile.displayName}"
                                )
                            }
                        }
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
            displayColor = ProfileColor.Default
        )
    }
}
