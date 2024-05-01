package fr.outadoc.pictochat.domain

import fr.outadoc.pictochat.UserProfile

data class Client(
    val endpointId: String,
    val profile: UserProfile
)
