package fr.outadoc.pictochat.domain

import fr.outadoc.pictochat.protocol.ChatPayload
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

interface ConnectionManager : Closeable {

    val state: StateFlow<State>
    val payloadFlow: SharedFlow<ChatPayload>

    suspend fun connect()

    fun sendPayload(endpointId: String, payload: ChatPayload)

    data class State(
        val connectedEndpoints: Set<String> = emptySet(),
    )
}
