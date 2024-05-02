package fr.outadoc.pictochat.preferences

import kotlinx.coroutines.flow.StateFlow

interface LocalPreferencesProvider {
    val preferences: StateFlow<LocalPreferences>
}
