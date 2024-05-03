package fr.outadoc.pictochat.domain

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Stable
data class RoomState(
    val id: Int,
    val displayName: String,
    val connectedDeviceIds: PersistentSet<RemoteDevice> = persistentSetOf(),
    val eventHistory: PersistentList<ChatEvent> = persistentListOf()
)
