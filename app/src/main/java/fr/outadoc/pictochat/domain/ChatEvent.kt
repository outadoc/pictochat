package fr.outadoc.pictochat.domain

import fr.outadoc.pictochat.preferences.DeviceId
import kotlinx.datetime.Instant

sealed class ChatEvent {
    abstract val timestamp: Instant

    data class Join(
        override val timestamp: Instant,
        val deviceId: DeviceId,
    ) : ChatEvent()

    data class Leave(
        override val timestamp: Instant,
        val deviceId: DeviceId,
    ) : ChatEvent()

    data class TextMessage(
        override val timestamp: Instant,
        val sender: DeviceId,
        val message: String,
    ) : ChatEvent()
}
