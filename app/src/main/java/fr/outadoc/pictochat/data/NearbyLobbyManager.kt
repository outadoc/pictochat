package fr.outadoc.pictochat.data

import fr.outadoc.pictochat.LocalPreferencesProvider
import fr.outadoc.pictochat.UserProfile
import fr.outadoc.pictochat.domain.ConnectionManager
import fr.outadoc.pictochat.domain.LobbyManager
import fr.outadoc.pictochat.domain.Room
import fr.outadoc.pictochat.protocol.ChatPayload
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
                )
            )
        )
    override val state = _state.asStateFlow()

    override fun join(room: Room) {
        val prefs = localPreferencesProvider.preferences.value
        val connectionState = connectionManager.state.value

        _state.update { state ->
            state.copy(joinedRoomId = room.id)
        }

        connectionState.connectedEndpoints.forEach { device ->
            connectionManager.sendPayload(
                endpointId = device.endpointId,
                payload = ChatPayload.Status(
                    displayName = prefs.userProfile.displayName,
                    displayColor = prefs.userProfile.displayColor,
                    roomId = room.id
                )
            )
        }
    }

    override fun leaveCurrentRoom() {
        val prefs = localPreferencesProvider.preferences.value
        val connectionState = connectionManager.state.value

        _state.update { state ->
            state.copy(joinedRoomId = null)
        }

        connectionState.connectedEndpoints.forEach { device ->
            connectionManager.sendPayload(
                endpointId = device.endpointId,
                payload = ChatPayload.Status(
                    displayName = prefs.userProfile.displayName,
                    displayColor = prefs.userProfile.displayColor,
                    roomId = null
                )
            )
        }
    }

    override fun sendMessage(message: String) {
        val connectionState = connectionManager.state.value

        val currentRoomId = checkNotNull(state.value.joinedRoomId) {
            "Cannot send message without first joining a room"
        }

        connectionState.connectedEndpoints.forEach { device ->
            connectionManager.sendPayload(
                endpointId = device.endpointId,
                payload = ChatPayload.TextMessage(
                    roomId = currentRoomId,
                    message = message
                )
            )
        }
    }

    override suspend fun connect() {
        coroutineScope {
            connectionManager.connect()

            launch {
                connectionManager.payloadFlow.collect { payload ->
                    processPayload(payload)
                }
            }
        }
    }

    private fun processPayload(payload: ReceivedPayload) {
        when (payload.data) {
            is ChatPayload.Status -> {
                _state.update { state ->
                    state.copy(
                        knownUsers = state.knownUsers
                            .put(
                                payload.sender.deviceId,
                                UserProfile(
                                    displayName = payload.data.displayName,
                                    displayColor = payload.data.displayColor
                                )
                            ),
                        availableRooms = state.availableRooms.map { room ->
                            if (room.id == payload.data.roomId) {
                                room.copy(
                                    connectedDeviceIds = room.connectedDeviceIds.add(payload.sender.deviceId)
                                )
                            } else {
                                room.copy(
                                    connectedDeviceIds = room.connectedDeviceIds.remove(payload.sender.deviceId)
                                )
                            }
                        }
                    )
                }
            }

            is ChatPayload.StatusRequest -> {
                val prefs = localPreferencesProvider.preferences.value
                connectionManager.sendPayload(
                    endpointId = payload.sender.endpointId,
                    payload = ChatPayload.Status(
                        displayName = prefs.userProfile.displayName,
                        displayColor = prefs.userProfile.displayColor,
                        roomId = state.value.joinedRoomId
                    )
                )
            }

            is ChatPayload.TextMessage -> {

            }
        }
    }

    override fun close() {
        connectionManager.close()
    }
}

