package fr.outadoc.pictochat.domain

import androidx.compose.runtime.Stable
import fr.outadoc.pictochat.preferences.DeviceId
import fr.outadoc.pictochat.preferences.UserProfile
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.Flow
import java.io.Closeable

interface LobbyManager : Closeable {

    @Stable
    data class State(
        val isOnline: Boolean,
        val userProfile: UserProfile,
        val knownProfiles: PersistentMap<DeviceId, UserProfile> = persistentMapOf(),
        val connectedEndpoints: ImmutableSet<RemoteDevice>,
        val rooms: PersistentMap<RoomId, RoomState>,
        val joinedRoomId: RoomId?,
        val nearbyUserCount: Int,
    )

    val state: Flow<State>

    suspend fun join(roomId: RoomId)
    suspend fun leaveCurrentRoom()
    suspend fun sendMessage(message: Message)

    suspend fun connect()
}
