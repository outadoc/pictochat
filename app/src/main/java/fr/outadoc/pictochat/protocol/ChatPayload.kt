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
    abstract val senderDeviceId: String

    @Serializable
    @SerialName("status_request")
    data class StatusRequest(
        @ProtoNumber(1)
        override val id: String,
        @ProtoNumber(2)
        override val senderDeviceId: String,
    ) : ChatPayload()

    @Serializable
    @SerialName("status")
    data class Status(
        @ProtoNumber(1)
        override val id: String,
        @ProtoNumber(2)
        override val senderDeviceId: String,
        @ProtoNumber(3)
        val displayName: String,
        @ProtoNumber(4)
        val displayColorId: Int,
        @ProtoNumber(5)
        val roomId: Int? = null,
    ) : ChatPayload()

    @Serializable
    @SerialName("message")
    data class Message(
        @ProtoNumber(1)
        override val id: String,
        @ProtoNumber(2)
        override val senderDeviceId: String,
        @ProtoNumber(3)
        val roomId: Int,
        @ProtoNumber(4)
        @Serializable(with = InstantIso8601Serializer::class)
        val sentAt: Instant,
        @ProtoNumber(5)
        val text: String,
        @ProtoNumber(6)
        val drawing: Drawing,
    ) : ChatPayload()
}
