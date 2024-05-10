package fr.outadoc.pictochat.domain

import fr.outadoc.pictochat.preferences.DeviceId
import kotlinx.datetime.Instant

sealed class ChatEvent {

    abstract val id: Int
    abstract val timestamp: Instant

    data class Join(
        override val id: Int,
        override val timestamp: Instant,
        val deviceId: DeviceId,
    ) : ChatEvent()

    data class Leave(
        override val id: Int,
        override val timestamp: Instant,
        val deviceId: DeviceId,
    ) : ChatEvent()

    data class Message(
        override val id: Int,
        override val timestamp: Instant,
        val sender: DeviceId,
        val message: fr.outadoc.pictochat.domain.Message,
    ) : ChatEvent()
}
