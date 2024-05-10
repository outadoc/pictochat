package fr.outadoc.pictochat.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import fr.outadoc.pictochat.randomInt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class DataStoreDeviceIdProvider(
    private val applicationContext: Context,
) : DeviceIdProvider {

    private var cachedDeviceId: DeviceId? = null

    override val deviceId: DeviceId
        get() = cachedDeviceId ?: DeviceId(readOrGenerateDeviceId())
            .also { cachedDeviceId = it }

    private fun readOrGenerateDeviceId(): Int = runBlocking {
        val deviceId = applicationContext.dataStore.data
            .map { prefs -> prefs[DEVICE_ID] }
            .first()

        deviceId ?: generateDeviceId().also { newDeviceId ->
            applicationContext.dataStore.edit { prefs ->
                prefs[DEVICE_ID] = newDeviceId
            }
        }
    }.also {
        Log.d("DeviceIdProvider", "Our device ID is: $it")
    }

    private fun generateDeviceId(): Int = randomInt()

    private companion object {
        val DEVICE_ID = intPreferencesKey("device_id_int")
    }
}
