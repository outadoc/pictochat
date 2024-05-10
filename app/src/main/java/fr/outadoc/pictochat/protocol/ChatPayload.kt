package fr.outadoc.pictochat.protocol

import fr.outadoc.pictochat.InstantMsTimestampSerializer
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
    val senderDeviceId: Int

    @Serializable
    @SerialName("hello")
    data class Hello(
        @ProtoNumber(1)
        override val id: Int,
        @ProtoNumber(2)
        override val senderDeviceId: Int,
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
        override val senderDeviceId: Int,
        @ProtoNumber(3)
        @Serializable(with = InstantMsTimestampSerializer::class)
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
        override val id: Int,
        @ProtoNumber(2)
        override val senderDeviceId: Int,
        @ProtoNumber(3)
        @Serializable(with = InstantMsTimestampSerializer::class)
        override val sentAt: Instant,
        @ProtoNumber(4)
        val roomId: Int,
        @ProtoNumber(5)
        val text: String,
        @ProtoNumber(6)
        val drawing: Drawing,
    ) : ChatPayload
}
