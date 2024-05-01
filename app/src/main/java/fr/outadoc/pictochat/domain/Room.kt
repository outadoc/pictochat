package fr.outadoc.pictochat.domain

data class Room(
    val id: Int,
    val displayName: String,
    val clients: List<Client> = emptyList()
)
