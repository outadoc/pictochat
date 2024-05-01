package fr.outadoc.pictochat.domain

import kotlinx.coroutines.flow.Flow

interface Room {
    val id: String
    val displayName: String
    fun getRoomState(): Flow<State>

    sealed class State {
        abstract val connectedClientCount: Int

        data class NotConnected(
            override val connectedClientCount: Int = 0,
        ) : State()

        data class Connected(
            val connectedClients: List<Client>,
        ) : State() {
            override val connectedClientCount: Int
                get() = connectedClients.size
        }
    }
}