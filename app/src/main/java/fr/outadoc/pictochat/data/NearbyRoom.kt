package fr.outadoc.pictochat.data

import fr.outadoc.pictochat.domain.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class NearbyRoom(
    override val id: String,
    override val displayName: String,
) : Room {

    override fun getRoomState(): Flow<Room.State> = flowOf()
}
