package fr.outadoc.pictochat.domain

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf

data class Room(
    val id: Int,
    val displayName: String,
    val connectedEndpointIds: PersistentSet<String> = persistentSetOf()
)
