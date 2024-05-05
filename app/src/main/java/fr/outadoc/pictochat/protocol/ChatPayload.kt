package fr.outadoc.pictochat.protocol

import kotlinx.datetime.Instant
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
sealed class ChatPayload {

    abstract val id: String

    @Serializable
    @SerialName("status_request")
    data class StatusRequest(
        @ProtoNumber(1)
        override val id: String,
    ) : ChatPayload()

    @Serializable
    @SerialName("status")
    data class Status(
        @ProtoNumber(1)
        override val id: String,
        @ProtoNumber(2)
        val displayName: String,
        @ProtoNumber(3)
        val displayColor: Int,
        @ProtoNumber(4)
        val roomId: Int? = null,
    ) : ChatPayload()

    @Serializable
    @SerialName("message")
    data class Message(
        @ProtoNumber(1)
        override val id: String,
        @ProtoNumber(2)
        val roomId: Int,
        @ProtoNumber(3)
        @Serializable(with = InstantIso8601Serializer::class)
        val sentAt: Instant,
        @ProtoNumber(4)
        val message: String,
        @ProtoNumber(5)
        val bitmap: ByteArray
    ) : ChatPayload() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Message

            if (id != other.id) return false
            if (roomId != other.roomId) return false
            if (sentAt != other.sentAt) return false
            if (message != other.message) return false
            if (!bitmap.contentEquals(other.bitmap)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + roomId
            result = 31 * result + sentAt.hashCode()
            result = 31 * result + message.hashCode()
            result = 31 * result + bitmap.contentHashCode()
            return result
        }
    }
}
