package fr.outadoc.pictochat.protocol

import fr.outadoc.pictochat.InstantMsTimestampSerializer
import fr.outadoc.pictochat.domain.RoomId
import fr.outadoc.pictochat.preferences.DeviceId
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
sealed interface ChatPayload {

    val id: Int
    val sentAt: Instant
    val source: DeviceId

    @Serializable
    @SerialName("hello")
    data class Hello(
        @ProtoNumber(1)
        override val id: Int,
        @ProtoNumber(2)
        override val source: DeviceId,
        @ProtoNumber(3)
        @Serializable(with = InstantMsTimestampSerializer::class)
        override val sentAt: Instant,
    ) : ChatPayload

    @Serializable
    @SerialName("status")
    data class Status(
        @ProtoNumber(1)
        override val id: Int,
        @ProtoNumber(2)
        override val source: DeviceId,
        @ProtoNumber(3)
        @Serializable(with = InstantMsTimestampSerializer::class)
        override val sentAt: Instant,
        @ProtoNumber(4)
        val displayName: String,
        @ProtoNumber(5)
        val displayColorId: Int,
        @ProtoNumber(6)
        val roomId: RoomId? = null,
    ) : ChatPayload

    @Serializable
    @SerialName("message")
    data class Message(
        @ProtoNumber(1)
        override val id: Int,
        @ProtoNumber(2)
        override val source: DeviceId,
        @ProtoNumber(3)
        @Serializable(with = InstantMsTimestampSerializer::class)
        override val sentAt: Instant,
        @ProtoNumber(4)
        val roomId: Int,
        @ProtoNumber(5)
        val contentDescription: String,
        @ProtoNumber(6)
        val drawing: ByteArray,
    ) : ChatPayload {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Message

            if (id != other.id) return false
            if (source != other.source) return false
            if (sentAt != other.sentAt) return false
            if (roomId != other.roomId) return false
            if (contentDescription != other.contentDescription) return false
            if (!drawing.contentEquals(other.drawing)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id
            result = 31 * result + source.hashCode()
            result = 31 * result + sentAt.hashCode()
            result = 31 * result + roomId
            result = 31 * result + contentDescription.hashCode()
            result = 31 * result + drawing.contentHashCode()
            return result
        }
    }
}
