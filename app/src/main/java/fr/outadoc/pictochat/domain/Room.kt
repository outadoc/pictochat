package fr.outadoc.pictochat.domain

data class Room(
    val id: String,
    val displayName: String,
    val clients: List<Client> = emptyList()
)
