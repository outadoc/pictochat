package fr.outadoc.pictochat.preferences

import fr.outadoc.pictochat.domain.ProfileColor

data class UserProfile(
    val displayName: String,
    val displayColor: ProfileColor,
)
