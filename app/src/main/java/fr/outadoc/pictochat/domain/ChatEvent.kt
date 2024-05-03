package fr.outadoc.pictochat.domain

import fr.outadoc.pictochat.preferences.DeviceId
import kotlinx.datetime.Instant

sealed class ChatEvent {
    abstract val timestamp: Instant

    data class TextMessage(
        val sender: DeviceId,
        val message: String,
        override val timestamp: Instant
    ) : ChatEvent()
}