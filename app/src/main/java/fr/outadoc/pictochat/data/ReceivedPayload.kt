package fr.outadoc.pictochat.data

import fr.outadoc.pictochat.domain.RemoteDevice
import fr.outadoc.pictochat.protocol.ChatPayload

data class ReceivedPayload(
    val sender: RemoteDevice,
    val data: ChatPayload
)
