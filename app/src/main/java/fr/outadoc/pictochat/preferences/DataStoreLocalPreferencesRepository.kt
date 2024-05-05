package fr.outadoc.pictochat.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.outadoc.pictochat.domain.ProfileColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreLocalPreferencesRepository(
    private val applicationContext: Context,
    private val deviceNameProvider: DeviceNameProvider,
) : LocalPreferencesRepository {

    override val preferences: Flow<LocalPreferences>
        get() = applicationContext.dataStore.data
            .map { preferences -> preferences.read() }

    override suspend fun updatePreferences(updated: LocalPreferences) {
        applicationContext.dataStore.edit { preferences ->
            preferences[KEY_USER_PROFILE_DISPLAY_NAME] = updated.userProfile.displayName
            preferences[KEY_USER_PROFILE_DISPLAY_COLOR] = updated.userProfile.displayColor.id
        }
    }

    private fun Preferences.read(): LocalPreferences {
        return LocalPreferences(
            userProfile = UserProfile(
                displayName = this[KEY_USER_PROFILE_DISPLAY_NAME]
                    ?: deviceNameProvider.getDeviceName(),
                displayColor = this[KEY_USER_PROFILE_DISPLAY_COLOR]
                    ?.let { ProfileColor.fromId(it) }
                    ?: ProfileColor.Default
            )
        )
    }

    private companion object {
        val KEY_USER_PROFILE_DISPLAY_NAME = stringPreferencesKey("user_profile_display_name")
        val KEY_USER_PROFILE_DISPLAY_COLOR = intPreferencesKey("user_profile_display_color")
    }
}