package fr.outadoc.pictochat

import androidx.annotation.ColorInt

data class UserProfile(
    val displayName: String,
    @ColorInt
    val displayColor: Int,
)
