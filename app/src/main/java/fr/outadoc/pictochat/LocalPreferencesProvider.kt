package fr.outadoc.pictochat

import kotlinx.coroutines.flow.StateFlow

interface LocalPreferencesProvider {
    val preferences: StateFlow<LocalPreferences>
}
