package fr.outadoc.pictochat.domain

import fr.outadoc.pictochat.preferences.UserProfile

data class Client(
    val endpointId: String,
    val profile: UserProfile
)
