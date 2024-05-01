package fr.outadoc.pictochat.domain

import fr.outadoc.pictochat.data.ReceivedPayload
import fr.outadoc.pictochat.protocol.ChatPayload
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

interface ConnectionManager : Closeable {

    val state: StateFlow<State>
    val payloadFlow: SharedFlow<ReceivedPayload>

    suspend fun connect()

    fun sendPayload(endpointId: String, payload: ChatPayload)

    /**
     * @property connectedEndpoints Maps endpoint IDs to device IDs.
     */
    data class State(
        val connectedEndpoints: PersistentSet<RemoteDevice> = persistentSetOf(),
        val connectingEndpoints: PersistentSet<RemoteDevice> = persistentSetOf(),
    )
}
