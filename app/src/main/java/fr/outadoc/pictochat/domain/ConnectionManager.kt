package fr.outadoc.pictochat.domain

import fr.outadoc.pictochat.protocol.ChatPayload
import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

interface ConnectionManager : Closeable {

    val state: StateFlow<State>

    suspend fun startDiscovery()
    suspend fun startAdvertising()

    fun sendPayload(endpointId: String, payload: ChatPayload)

    fun stopDiscovery()
    fun stopAdvertising()

    data class State(
        val connectedClients: List<Client> = emptyList()
    )
}
