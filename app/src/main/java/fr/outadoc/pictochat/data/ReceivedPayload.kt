package fr.outadoc.pictochat.data

import fr.outadoc.pictochat.protocol.ChatPayload

data class ReceivedPayload(
    val senderEndpointId: String,
    val data: ChatPayload
)
