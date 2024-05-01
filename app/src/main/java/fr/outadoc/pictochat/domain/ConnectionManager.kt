package fr.outadoc.pictochat.domain

import fr.outadoc.pictochat.data.ReceivedPayload
import fr.outadoc.pictochat.protocol.ChatPayload
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
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

data class RemoteDevice(
    val endpointId: String,
    val deviceId: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RemoteDevice

        // If either the endpoint ID or the device ID matches, we consider the devices equal
        return endpointId == other.endpointId || deviceId == other.deviceId
    }

    override fun hashCode(): Int {
        var result = endpointId.hashCode()
        result = 31 * result + deviceId.hashCode()
        return result
    }
}
