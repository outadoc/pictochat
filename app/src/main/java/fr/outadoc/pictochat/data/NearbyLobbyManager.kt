package fr.outadoc.pictochat.data

import fr.outadoc.pictochat.LocalPreferencesProvider
import fr.outadoc.pictochat.domain.ConnectionManager
import fr.outadoc.pictochat.domain.LobbyManager
import fr.outadoc.pictochat.domain.Room
import fr.outadoc.pictochat.protocol.ChatPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NearbyLobbyManager(
    private val connectionManager: ConnectionManager,
    private val localPreferencesProvider: LocalPreferencesProvider,
) : LobbyManager {

    private val _state: MutableStateFlow<LobbyManager.State> =
        MutableStateFlow(
            LobbyManager.State(
                availableRooms = listOf(
                    Room(id = 0, displayName = "Room A"),
                    Room(id = 1, displayName = "Room B"),
                    Room(id = 2, displayName = "Room C"),
                    Room(id = 3, displayName = "Room D"),
                ),
                joinedRoom = null
            )
        )
    override val state = _state.asStateFlow()

    private val stateMutex = Mutex()

    override suspend fun join(room: Room) {
        stateMutex.withLock {
            val prefs = localPreferencesProvider.preferences.value
            val connectionState = connectionManager.state.value

            _state.update { state ->
                state.copy(joinedRoom = room)
            }

            connectionState.connectedEndpoints.forEach { endpointId ->
                connectionManager.sendPayload(
                    endpointId = endpointId,
                    payload = ChatPayload.Status(
                        displayName = prefs.userProfile.displayName,
                        displayColor = prefs.userProfile.displayColor,
                        roomId = room.id
                    )
                )
            }
        }
    }

    override suspend fun leaveCurrentRoom() {
        stateMutex.withLock {
            val prefs = localPreferencesProvider.preferences.value
            val connectionState = connectionManager.state.value

            _state.update { state ->
                state.copy(joinedRoom = null)
            }

            connectionState.connectedEndpoints.forEach { client ->
                connectionManager.sendPayload(
                    endpointId = client,
                    payload = ChatPayload.Status(
                        displayName = prefs.userProfile.displayName,
                        displayColor = prefs.userProfile.displayColor,
                        roomId = null
                    )
                )
            }
        }
    }

    override suspend fun sendMessage(message: String) {
        val connectionState = connectionManager.state.value

        val currentRoom = checkNotNull(state.value.joinedRoom) {
            "Cannot send message without finirs joining a room"
        }

        connectionState.connectedEndpoints.forEach { client ->
            connectionManager.sendPayload(
                endpointId = client,
                payload = ChatPayload.TextMessage(
                    roomId = currentRoom.id,
                    message = message
                )
            )
        }
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

