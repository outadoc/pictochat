package fr.outadoc.pictochat.data

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
import fr.outadoc.pictochat.randomInt
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
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

class DefaultLobbyManager(
    localPreferencesRepository: LocalPreferencesRepository,
    private val connectionManager: ConnectionManager,
    private val deviceIdProvider: DeviceIdProvider,
    private val clock: Clock,
) : LobbyManager {

    private data class UpdatedUserProfile(
        val updatedAt: Instant,
        val userProfile: UserProfile,
    )

    private data class ProfileBroadcastTrigger(
        val userProfile: UserProfile,
        val joinedRoomId: RoomId?,
        val connectedPeers: ImmutableSet<DeviceId>,
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
                connectedPeers = connectionState.connectedPeers,
                userProfile = userProfile,
                knownProfiles = internalState.knownProfiles
                    .mapValues { it.value.userProfile }
                    .toPersistentMap()
                    .put(deviceIdProvider.deviceId, userProfile),
                joinedRoomId = internalState.joinedRoomId,
                rooms = internalState.rooms
                    .mapValues { (_, roomState) ->
                        roomState.copy(
                            connectedDevices = roomState.connectedDevices
                                .filter { deviceId ->
                                    val isOwnDevice = deviceId == deviceIdProvider.deviceId
                                    val isKnownPeer =
                                        connectionState.connectedPeers.contains(deviceId)
                                    isOwnDevice || isKnownPeer
                                }
                                .toPersistentSet()
                        )
                    }
                    .toPersistentMap()
            )
        }
            .distinctUntilChanged()

    private var connectionJob: Job? = null

    override suspend fun join(roomId: RoomId) {
        check(_state.value.rooms.containsKey(roomId)) {
            "Room $roomId does not exist"
        }

        _state.update { state ->
            state.copy(
                joinedRoomId = roomId,
            )
        }
    }

    override suspend fun leaveCurrentRoom() {
        _state.update { state ->
            state.copy(
                joinedRoomId = null
            )
        }
    }

    override suspend fun sendMessage(message: Message) {
        val currentRoomId = checkNotNull(_state.value.joinedRoomId) {
            "Cannot send message without first joining a room"
        }

        val payload = ChatPayload.Message(
            id = randomInt(),
            source = deviceIdProvider.deviceId,
            sentAt = clock.now(),
            roomId = currentRoomId.value,
            contentDescription = message.contentDescription,
            drawing = message.drawing
        )

        // Send the message to all connected devices
        connectionManager.broadcast(payload = payload)
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
                    state
                        .map { state ->
                            ProfileBroadcastTrigger(
                                userProfile = state.userProfile,
                                joinedRoomId = state.joinedRoomId,
                                connectedPeers = state.connectedPeers
                            )
                        }
                        .distinctUntilChanged()
                        .collect { trigger ->
                            connectionManager.broadcast(
                                payload = ChatPayload.Status(
                                    id = randomInt(),
                                    source = deviceIdProvider.deviceId,
                                    sentAt = clock.now(),
                                    displayName = trigger.userProfile.displayName,
                                    displayColorId = trigger.userProfile.displayColor.id,
                                    roomId = trigger.joinedRoomId
                                )
                            )
                        }
                }
            }
        }
    }

    private fun processPayload(payload: ChatPayload) {
        when (payload) {
            is ChatPayload.Hello -> {}
            is ChatPayload.Status -> {
                processReceivedStatus(payload = payload)
            }

            is ChatPayload.Message -> {
                processReceivedMessage(payload = payload)
            }
        }
    }

    private fun processReceivedStatus(payload: ChatPayload.Status) {
        _state.update { state ->
            val sender = payload.source
            val existingProfile: UpdatedUserProfile? = state.knownProfiles[sender]

            if (existingProfile != null && existingProfile.updatedAt > payload.sentAt) {
                return@update state
            }

            state.copy(
                // Remember the user's profile
                knownProfiles = state.knownProfiles
                    .put(
                        sender,
                        UpdatedUserProfile(
                            updatedAt = payload.sentAt,
                            UserProfile(
                                displayName = payload.displayName,
                                displayColor = ProfileColor.fromId(payload.displayColorId)
                            )
                        )
                    ),
                // Update the rooms' connected devices
                rooms = state.rooms
                    .mapValues { (id, roomState) ->
                        when (id) {
                            payload.roomId -> {
                                if (roomState.connectedDevices.contains(sender)) {
                                    // The device is already known to be in the room
                                    roomState
                                } else {
                                    // The device is new to the room
                                    roomState.copy(
                                        connectedDevices = roomState.connectedDevices
                                            .add(sender),
                                        eventHistoryIds = roomState.eventHistoryIds
                                            .add(payload.id),
                                        eventHistory = roomState.eventHistory.add(
                                            ChatEvent.Join(
                                                id = payload.id,
                                                timestamp = payload.sentAt,
                                                deviceId = sender,
                                            )
                                        )
                                    )
                                }
                            }

                            else -> {
                                if (roomState.connectedDevices.contains(sender)) {
                                    // The device was in the room and is leaving
                                    roomState.copy(
                                        connectedDevices = roomState.connectedDevices
                                            .remove(sender),
                                        eventHistoryIds = roomState.eventHistoryIds
                                            .add(payload.id),
                                        eventHistory = roomState.eventHistory.add(
                                            ChatEvent.Leave(
                                                id = payload.id,
                                                timestamp = payload.sentAt,
                                                deviceId = sender,
                                            )
                                        ),
                                    )
                                } else {
                                    // The device is not in this room
                                    roomState
                                }
                            }
                        }
                    }
                    .toPersistentMap()
            )
        }
    }

    private fun processReceivedMessage(payload: ChatPayload.Message) {
        _state.update { state ->
            val roomId = RoomId(payload.roomId)
            val roomState = state.rooms[roomId]

            checkNotNull(roomState) {
                "Received message for unknown room ${payload.roomId}"
            }

            // If the message is already in the room's event history, ignore it
            if (roomState.eventHistoryIds.contains(payload.id)) {
                return@update state
            }

            state.copy(
                // Add the message to the room's event history
                rooms = state.rooms.put(
                    roomId,
                    roomState.copy(
                        eventHistoryIds = roomState.eventHistoryIds.add(payload.id),
                        eventHistory = roomState.eventHistory.add(
                            ChatEvent.Message(
                                id = payload.id,
                                timestamp = payload.sentAt,
                                sender = payload.source,
                                message = Message(
                                    contentDescription = payload.contentDescription,
                                    drawing = payload.drawing
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
