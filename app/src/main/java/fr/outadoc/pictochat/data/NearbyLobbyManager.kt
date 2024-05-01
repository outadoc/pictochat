package fr.outadoc.pictochat.data

import fr.outadoc.pictochat.domain.ConnectionManager
import fr.outadoc.pictochat.domain.LobbyManager
import fr.outadoc.pictochat.domain.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NearbyLobbyManager(
    private val connectionManager: ConnectionManager,
) : LobbyManager {

    private val _state: MutableStateFlow<LobbyManager.State> =
        MutableStateFlow(
            LobbyManager.State(
                availableRooms = listOf(
                    Room(id = "a", displayName = "Room A"),
                    Room(id = "b", displayName = "Room B"),
                    Room(id = "c", displayName = "Room C"),
                    Room(id = "d", displayName = "Room D"),
                ),
                joinedRoom = null
            )
        )
    override val state = _state.asStateFlow()

    private val stateMutex = Mutex()

    override suspend fun join(room: Room) {
        stateMutex.withLock {
            doLeaveCurrentRoom()
            // TODO send room join payload
            _state.update { state ->
                state.copy(joinedRoom = room)
            }
        }
    }

    override suspend fun leaveCurrentRoom(room: Room) {
        stateMutex.withLock {
            _state.update { state ->
                state.copy(joinedRoom = null)
            }

            doLeaveCurrentRoom()
        }
    }

    private suspend fun doLeaveCurrentRoom() {
        // TODO send room leave payload
    }

    override suspend fun connect() {
        connectionManager.startAdvertising()
        connectionManager.startDiscovery()
    }

    override fun disconnect() {
        connectionManager.stopAdvertising()
        connectionManager.stopDiscovery()
    }

    override fun close() = disconnect()
}

