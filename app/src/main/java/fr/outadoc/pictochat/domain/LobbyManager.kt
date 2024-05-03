package fr.outadoc.pictochat.domain

import androidx.compose.runtime.Stable
import fr.outadoc.pictochat.preferences.DeviceId
import fr.outadoc.pictochat.preferences.UserProfile
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

interface LobbyManager : Closeable {

    @Stable
    data class State(
        val rooms: PersistentList<RoomState>,
        val knownProfiles: PersistentMap<DeviceId, UserProfile> = persistentMapOf(),
        val nearbyUserCount: Int = 0,
        val joinedRoomId: Int? = null,
    )

    val state: StateFlow<State>

    suspend fun join(roomId: Int)
    suspend fun leaveCurrentRoom()
    suspend fun sendMessage(message: String)

    suspend fun connect()
}
