package fr.outadoc.pictochat.protocol

import kotlinx.datetime.Instant
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
sealed interface ChatPayload {

    val id: String
    val sentAt: Instant
    val senderDeviceId: String

    @Serializable
    @SerialName("status")
    data class Status(
        @ProtoNumber(1)
        override val id: String,
        @ProtoNumber(2)
        override val senderDeviceId: String,
        @ProtoNumber(3)
        @Serializable(with = InstantIso8601Serializer::class)
        override val sentAt: Instant,
        @ProtoNumber(4)
        val displayName: String,
        @ProtoNumber(5)
        val displayColorId: Int,
        @ProtoNumber(6)
        val roomId: Int? = null,
    ) : ChatPayload

    @Serializable
    @SerialName("message")
    data class Message(
        @ProtoNumber(1)
        override val id: String,
        @ProtoNumber(2)
        override val senderDeviceId: String,
        @ProtoNumber(3)
        @Serializable(with = InstantIso8601Serializer::class)
        override val sentAt: Instant,
        @ProtoNumber(4)
        val roomId: Int,
        @ProtoNumber(5)
        val text: String,
        @ProtoNumber(6)
        val drawing: Drawing,
    ) : ChatPayload
}
