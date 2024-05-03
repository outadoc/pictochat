package fr.outadoc.pictochat.domain

import androidx.compose.runtime.Stable
import fr.outadoc.pictochat.preferences.DeviceId
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Stable
data class RoomState(
    val id: RoomId,
    val displayName: String,
    val connectedDevices: PersistentSet<DeviceId> = persistentSetOf(),
    val eventHistory: PersistentList<ChatEvent> = persistentListOf()
)
