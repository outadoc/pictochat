package fr.outadoc.pictochat.domain

import fr.outadoc.pictochat.preferences.DeviceId
import fr.outadoc.pictochat.protocol.ChatPayload
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import java.io.Closeable

interface ConnectionManager : Closeable {

    val state: Flow<State>
    val payloadFlow: SharedFlow<ChatPayload>

    suspend fun connect()

    suspend fun broadcast(payload: ChatPayload)

    data class State(
        val isOnline: Boolean = false,
        val connectedPeers: ImmutableSet<DeviceId> = persistentSetOf(),
    )
}
