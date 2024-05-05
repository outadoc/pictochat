package fr.outadoc.pictochat.preferences

import kotlinx.coroutines.flow.Flow

interface LocalPreferencesRepository {
    val preferences: Flow<LocalPreferences>
    suspend fun updatePreferences(updated: LocalPreferences)
}
