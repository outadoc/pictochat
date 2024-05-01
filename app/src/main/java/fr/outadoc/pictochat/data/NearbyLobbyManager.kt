package fr.outadoc.pictochat.data

import fr.outadoc.pictochat.domain.ConnectionManager
import fr.outadoc.pictochat.domain.LobbyManager
import fr.outadoc.pictochat.domain.Room

class NearbyLobbyManager(
    private val connectionManager: ConnectionManager,
) : LobbyManager {



    private val _rooms: List<NearbyRoom> = listOf(
        NearbyRoom(id = "a", displayName = "Room A"),
        NearbyRoom(id = "b", displayName = "Room B"),
    )

    override val rooms: List<Room> get() = _rooms

    override suspend fun join(room: Room) {
        check(room is NearbyRoom && room in _rooms) { "Room not found" }
        connectionManager.startAdvertising()
    }

    override suspend fun leave(room: Room) {
        check(room is NearbyRoom && room in _rooms) { "Room not found" }
        connectionManager.startAdvertising()
    }

    override suspend fun startRoomDiscovery() {
        connectionManager.startDiscovery()
    }

    override suspend fun stopRoomDiscovery() {
        connectionManager.stopDiscovery()
    }

    override fun close() {
        connectionManager.close()
    }
}

