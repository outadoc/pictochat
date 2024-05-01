package fr.outadoc.pictochat.domain

import java.io.Closeable

interface LobbyManager : Closeable {

    val rooms: List<Room>

    suspend fun join(room: Room)
    suspend fun leave(room: Room)

    suspend fun startRoomDiscovery()
    suspend fun stopRoomDiscovery()
}

