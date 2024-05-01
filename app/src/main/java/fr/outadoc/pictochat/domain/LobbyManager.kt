package fr.outadoc.pictochat.domain

import java.io.Closeable

interface LobbyManager : Closeable {

    data class State(
        val availableRooms: List<Room>,
        val joinedRoom: Room?
    )

    val rooms: List<Room>

    suspend fun join(room: Room)
    suspend fun leave(room: Room)

    suspend fun startRoomDiscovery()
    suspend fun stopRoomDiscovery()
}

