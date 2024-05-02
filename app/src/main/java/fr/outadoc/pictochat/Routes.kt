package fr.outadoc.pictochat

import kotlinx.serialization.Serializable

sealed interface Route {

    @Serializable
    data object Home : Route

    @Serializable
    data class Room(val roomId: Int) : Route
}
