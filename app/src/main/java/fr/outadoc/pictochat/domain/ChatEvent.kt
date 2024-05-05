package fr.outadoc.pictochat.domain

import fr.outadoc.pictochat.preferences.DeviceId
import kotlinx.datetime.Instant

sealed class ChatEvent {

    abstract val id: String
    abstract val timestamp: Instant

    data class Join(
        override val id: String,
        override val timestamp: Instant,
        val deviceId: DeviceId,
    ) : ChatEvent()

    data class Leave(
        override val id: String,
        override val timestamp: Instant,
        val deviceId: DeviceId,
    ) : ChatEvent()

    data class Message(
        override val id: String,
        override val timestamp: Instant,
        val sender: DeviceId,
        val message: String,
        val bitmap: ByteArray
    ) : ChatEvent() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Message

            if (id != other.id) return false
            if (timestamp != other.timestamp) return false
            if (sender != other.sender) return false
            if (message != other.message) return false
            if (!bitmap.contentEquals(other.bitmap)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + timestamp.hashCode()
            result = 31 * result + sender.hashCode()
            result = 31 * result + message.hashCode()
            result = 31 * result + bitmap.contentHashCode()
            return result
        }
    }
}
