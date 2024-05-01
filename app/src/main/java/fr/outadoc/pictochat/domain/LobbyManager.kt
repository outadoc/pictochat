package fr.outadoc.pictochat.domain

import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

interface LobbyManager : Closeable {

    data class State(
        val availableRooms: List<Room>,
        val joinedRoom: Room?,
    )

    val state: StateFlow<State>

    suspend fun join(room: Room)
    suspend fun leaveCurrentRoom(room: Room)

    suspend fun connect()
    fun disconnect()
}

