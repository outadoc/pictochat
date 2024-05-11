package fr.outadoc.pictochat.domain

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class RoomId(val value: Int)
