package fr.outadoc.pictochat.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {

    @Serializable
    data object Home : Route

    @Serializable
    data class Room(val roomId: Int) : Route

    @Serializable
    data object Settings : Route
}
