package fr.outadoc.pictochat.protocol

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
sealed class ChatPayload {

    @Serializable
    @SerialName("status_request")
    data object StatusRequest : ChatPayload()

    @Serializable
    @SerialName("status")
    data class Status(
        @ProtoNumber(1)
        val displayName: String,
        @ProtoNumber(2)
        val displayColor: Int,
        @ProtoNumber(3)
        val roomId: Int? = null,
    ) : ChatPayload()

    @Serializable
    @SerialName("message_text")
    data class TextMessage(
        @ProtoNumber(1)
        val message: String,
        @ProtoNumber(2)
        val roomId: Int,
    ) : ChatPayload()
}
