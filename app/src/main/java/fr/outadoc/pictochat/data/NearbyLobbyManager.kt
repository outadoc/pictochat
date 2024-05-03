package fr.outadoc.pictochat.data

import fr.outadoc.pictochat.domain.ChatEvent
import fr.outadoc.pictochat.domain.ConnectionManager
import fr.outadoc.pictochat.domain.LobbyManager
import fr.outadoc.pictochat.domain.RoomId
import fr.outadoc.pictochat.domain.RoomState
import fr.outadoc.pictochat.preferences.LocalPreferencesProvider
import fr.outadoc.pictochat.preferences.UserProfile
import fr.outadoc.pictochat.protocol.ChatPayload
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.UUID

class NearbyLobbyManager(
    private val connectionManager: ConnectionManager,
    private val localPreferencesProvider: LocalPreferencesProvider,
    private val clock: Clock,
) : LobbyManager {

    private val _state: MutableStateFlow<LobbyManager.State> =
        MutableStateFlow(
            LobbyManager.State(
                rooms = persistentMapOf(
                    RoomId(0) to RoomState(id = RoomId(0), displayName = "Room A"),
                    RoomId(1) to RoomState(id = RoomId(1), displayName = "Room B"),
                    RoomId(2) to RoomState(id = RoomId(2), displayName = "Room C"),
                    RoomId(3) to RoomState(id = RoomId(3), displayName = "Room D"),
                )
            )
        )
    override val state = _state.asStateFlow()

    override suspend fun join(roomId: RoomId) {
        val prefs = localPreferencesProvider.preferences.value
        val connectionState = connectionManager.state.value

        check(state.value.rooms.containsKey(roomId)) {
            "Room $roomId does not exist"
        }

        _state.update { state ->
            state.copy(joinedRoomId = roomId)
        }

        val payload = ChatPayload.Status(
            id = UUID.randomUUID().toString(),
            displayName = prefs.userProfile.displayName,
            displayColor = prefs.userProfile.displayColor,
            roomId = roomId.value
        )

        connectionState.connectedEndpoints.forEach { device ->
            connectionManager.sendPayload(
                endpointId = device.endpointId,
                payload = payload
            )
        }
    }

    override suspend fun leaveCurrentRoom() {
        val prefs = localPreferencesProvider.preferences.value
        val connectionState = connectionManager.state.value

        _state.update { state ->
            state.copy(joinedRoomId = null)
        }

        val payload = ChatPayload.Status(
            id = UUID.randomUUID().toString(),
            displayName = prefs.userProfile.displayName,
            displayColor = prefs.userProfile.displayColor,
            roomId = null
        )

        connectionState.connectedEndpoints.forEach { device ->
            connectionManager.sendPayload(
                endpointId = device.endpointId,
                payload = payload
            )
        }
    }

    override suspend fun sendMessage(message: String) {
        val connectionState = connectionManager.state.value

        val currentRoomId = checkNotNull(state.value.joinedRoomId) {
            "Cannot send message without first joining a room"
        }

        val payload = ChatPayload.TextMessage(
            id = UUID.randomUUID().toString(),
            sentAt = clock.now(),
            roomId = currentRoomId.value,
            message = message
        )

        connectionState.connectedEndpoints.forEach { device ->
            connectionManager.sendPayload(
                endpointId = device.endpointId,
                payload = payload
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

            launch {
                connectionManager.state.collect { connectionState ->
                    _state.update { state ->
                        state.copy(
                            nearbyUserCount = connectionState.connectedEndpoints.size
                        )
                    }
                }
            }
        }
    }

    private suspend fun processPayload(payload: ReceivedPayload) {
        when (payload.data) {
            is ChatPayload.Status -> {
                _state.update { state ->
                    state.copy(
                        knownProfiles = state.knownProfiles
                            .put(
                                payload.sender.deviceId,
                                UserProfile(
                                    displayName = payload.data.displayName,
                                    displayColor = payload.data.displayColor
                                )
                            ),
                        rooms = state.rooms
                            .mapValues { (id, state) ->
                                when (id.value) {
                                    payload.data.roomId -> {
                                        state.copy(
                                            connectedDevices = state.connectedDevices.add(
                                                payload.sender.deviceId
                                            )
                                        )
                                    }

                                    else -> {
                                        state.copy(
                                            connectedDevices = state.connectedDevices.remove(
                                                payload.sender.deviceId
                                            )
                                        )
                                    }
                                }
                            }
                            .toPersistentMap()
                    )
                }
            }

            is ChatPayload.StatusRequest -> {
                val prefs = localPreferencesProvider.preferences.value
                connectionManager.sendPayload(
                    endpointId = payload.sender.endpointId,
                    payload = ChatPayload.Status(
                        id = UUID.randomUUID().toString(),
                        displayName = prefs.userProfile.displayName,
                        displayColor = prefs.userProfile.displayColor,
                        roomId = state.value.joinedRoomId?.value
                    )
                )
            }

            is ChatPayload.TextMessage -> {
                _state.update { state ->
                    state.copy(
                        rooms = state.rooms
                            .mapValues { (id, roomState) ->
                                if (id.value == payload.data.roomId) {
                                    roomState.copy(
                                        eventHistory = roomState.eventHistory.add(
                                            ChatEvent.TextMessage(
                                                id = payload.data.id,
                                                timestamp = payload.data.sentAt,
                                                sender = payload.sender.deviceId,
                                                message = payload.data.message
                                            )
                                        )
                                    )
                                } else {
                                    roomState
                                }
                            }
                            .toPersistentMap()
                    )
                }
            }
        }
    }

    override fun close() {
        connectionManager.close()
    }
}
