package fr.outadoc.pictochat.data

import androidx.compose.ui.util.fastAny
import fr.outadoc.pictochat.domain.ChatEvent
import fr.outadoc.pictochat.domain.ConnectionManager
import fr.outadoc.pictochat.domain.LobbyManager
import fr.outadoc.pictochat.domain.Message
import fr.outadoc.pictochat.domain.ProfileColor
import fr.outadoc.pictochat.domain.RoomId
import fr.outadoc.pictochat.domain.RoomState
import fr.outadoc.pictochat.preferences.DeviceId
import fr.outadoc.pictochat.preferences.DeviceIdProvider
import fr.outadoc.pictochat.preferences.LocalPreferencesRepository
import fr.outadoc.pictochat.preferences.UserProfile
import fr.outadoc.pictochat.protocol.ChatPayload
import fr.outadoc.pictochat.protocol.Drawing
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID

class NearbyLobbyManager(
    localPreferencesRepository: LocalPreferencesRepository,
    private val connectionManager: ConnectionManager,
    private val deviceIdProvider: DeviceIdProvider,
    private val clock: Clock,
) : LobbyManager {

    private data class UpdatedUserProfile(
        val updatedAt: Instant,
        val userProfile: UserProfile,
    )

    private data class InternalState(
        val rooms: PersistentMap<RoomId, RoomState> = persistentMapOf(
            RoomId(0) to RoomState(id = RoomId(0), displayName = "Room A"),
            RoomId(1) to RoomState(id = RoomId(1), displayName = "Room B"),
            RoomId(2) to RoomState(id = RoomId(2), displayName = "Room C"),
            RoomId(3) to RoomState(id = RoomId(3), displayName = "Room D"),
        ),
        val knownProfiles: PersistentMap<DeviceId, UpdatedUserProfile> = persistentMapOf(),
        val joinedRoomId: RoomId? = null,
    )

    private val _state: MutableStateFlow<InternalState> = MutableStateFlow(InternalState())

    override val state: Flow<LobbyManager.State> =
        combine(
            connectionManager.state,
            localPreferencesRepository.preferences.map { it.userProfile },
            _state
        ) { connectionState, userProfile, internalState ->
            LobbyManager.State(
                isOnline = connectionState.isOnline,
                connectedEndpoints = connectionState.connectedEndpoints,
                userProfile = userProfile,
                knownProfiles = internalState.knownProfiles
                    .mapValues { it.value.userProfile }
                    .toPersistentMap()
                    .put(deviceIdProvider.deviceId, userProfile),
                nearbyUserCount = connectionState.connectedEndpoints.size,
                joinedRoomId = internalState.joinedRoomId,
                rooms = internalState.rooms
            )
        }
            .distinctUntilChanged()

    private var connectionJob: Job? = null

    override suspend fun join(roomId: RoomId) {
        check(_state.value.rooms.containsKey(roomId)) {
            "Room $roomId does not exist"
        }

        _state.update { state ->
            state.copy(joinedRoomId = roomId)
        }
    }

    override suspend fun leaveCurrentRoom() {
        _state.update { state ->
            state.copy(joinedRoomId = null)
        }
    }

    override suspend fun sendMessage(message: Message) {
        val connectionState = connectionManager.state.value

        val currentRoomId = checkNotNull(_state.value.joinedRoomId) {
            "Cannot send message without first joining a room"
        }

        val payload = ChatPayload.Message(
            id = UUID.randomUUID().toString(),
            senderDeviceId = deviceIdProvider.deviceId.value,
            sentAt = clock.now(),
            roomId = currentRoomId.value,
            text = message.contentDescription,
            drawing = Drawing(
                width = message.bitmapWidth,
                height = message.bitmapHeight,
                data = message.bitmap
            )
        )

        // Send the message to all connected devices
        connectionState.connectedEndpoints.forEach { device ->
            connectionManager.sendPayload(
                endpointId = device.endpointId,
                payload = payload
            )
        }

        // Also show the message locally
        processReceivedMessage(
            sender = deviceIdProvider.deviceId,
            payload = payload
        )
    }

    override suspend fun connect() {
        connectionJob?.cancel()
        coroutineScope {
            connectionJob = launch {
                launch { connectionManager.connect() }

                launch {
                    connectionManager.payloadFlow.collect { payload ->
                        processPayload(payload)
                    }
                }

                launch {
                    state.collect { state ->
                        val payload = ChatPayload.Status(
                            id = UUID.randomUUID().toString(),
                            senderDeviceId = deviceIdProvider.deviceId.value,
                            sentAt = clock.now(),
                            displayName = state.userProfile.displayName,
                            displayColorId = state.userProfile.displayColor.id,
                            roomId = state.joinedRoomId?.value
                        )

                        state.connectedEndpoints.forEach { endpoint ->
                            connectionManager.sendPayload(
                                endpointId = endpoint.endpointId,
                                payload = payload
                            )
                        }
                    }
                }
            }
        }
    }

    private fun processPayload(payload: ReceivedPayload) {
        when (payload.data) {
            is ChatPayload.Status -> {
                _state.update { state ->
                    val sender = DeviceId(payload.data.senderDeviceId)
                    val existingProfile: UpdatedUserProfile? = state.knownProfiles[sender]

                    if (existingProfile != null && existingProfile.updatedAt > payload.data.sentAt) {
                        return@update state
                    }

                    state.copy(
                        // Remember the user's profile
                        knownProfiles = state.knownProfiles
                            .put(
                                sender,
                                UpdatedUserProfile(
                                    updatedAt = payload.data.sentAt,
                                    UserProfile(
                                        displayName = payload.data.displayName,
                                        displayColor = ProfileColor.fromId(payload.data.displayColorId)
                                    )
                                )
                            ),
                        // Update the rooms' connected devices
                        rooms = state.rooms
                            .mapValues { (id, state) ->
                                when (id.value) {
                                    payload.data.roomId -> {
                                        state.copy(
                                            connectedDevices = state.connectedDevices.add(sender)
                                        )
                                    }

                                    else -> {
                                        state.copy(
                                            connectedDevices = state.connectedDevices.remove(sender)
                                        )
                                    }
                                }
                            }
                            .toPersistentMap()
                    )
                }
            }

            is ChatPayload.Message -> {
                // Received a message from someone else, process it
                processReceivedMessage(
                    sender = payload.sender.deviceId,
                    payload = payload.data
                )
            }
        }
    }

    private fun processReceivedMessage(sender: DeviceId, payload: ChatPayload.Message) {
        _state.update { state ->
            val roomId = RoomId(payload.roomId)
            val roomState = state.rooms[roomId]

            checkNotNull(roomState) {
                "Received message for unknown room ${payload.roomId}"
            }

            // If the message is already in the room's event history, ignore it
            if (state.rooms[roomId]?.eventHistory?.fastAny { it.id == payload.id } == true) {
                return@update state
            }

            state.copy(
                // Add the message to the room's event history
                rooms = state.rooms.put(
                    roomId,
                    roomState.copy(
                        eventHistory = roomState.eventHistory.add(
                            ChatEvent.Message(
                                id = payload.id,
                                timestamp = payload.sentAt,
                                sender = sender,
                                message = Message(
                                    contentDescription = payload.text,
                                    bitmapWidth = payload.drawing.width,
                                    bitmapHeight = payload.drawing.height,
                                    bitmap = payload.drawing.data
                                )
                            )
                        )
                    )
                )
            )
        }
    }

    override fun close() {
        connectionJob?.cancel()
    }
}
