package fr.outadoc.pictochat.domain

import fr.outadoc.pictochat.UserProfile
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

interface LobbyManager : Closeable {

    data class State(
        val rooms: List<Room>,
        val knownUsers: PersistentMap<String, UserProfile> = persistentMapOf(),
        val joinedRoomId: Int? = null,
    )

    val state: StateFlow<State>

    suspend fun join(room: Room)
    suspend fun leaveCurrentRoom()
    suspend fun sendMessage(message: String)

    suspend fun connect()
}
