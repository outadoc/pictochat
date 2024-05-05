package fr.outadoc.pictochat

import android.app.Application
import android.content.Context
import android.os.StrictMode
import fr.outadoc.pictochat.data.NearbyConnectionManager
import fr.outadoc.pictochat.data.NearbyLobbyManager
import fr.outadoc.pictochat.domain.ConnectionManager
import fr.outadoc.pictochat.domain.LobbyManager
import fr.outadoc.pictochat.domain.ProfileColor
import fr.outadoc.pictochat.preferences.AndroidDeviceNameProvider
import fr.outadoc.pictochat.preferences.DataStoreDeviceIdProvider
import fr.outadoc.pictochat.preferences.DeviceIdProvider
import fr.outadoc.pictochat.preferences.DeviceNameProvider
import fr.outadoc.pictochat.preferences.LocalPreferences
import fr.outadoc.pictochat.preferences.LocalPreferencesRepository
import fr.outadoc.pictochat.preferences.UserProfile
import fr.outadoc.pictochat.ui.navigation.MainViewModel
import fr.outadoc.pictochat.ui.room.RoomViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import org.koin.core.context.startKoin
import org.koin.dsl.module

class Application : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults()
        }

        startKoin {
            modules(commonModule)
        }
    }

    private val commonModule = module {
        single<Context> { applicationContext }
        single<Clock> { Clock.System }
        single<DeviceNameProvider> { AndroidDeviceNameProvider(get()) }
        single<DeviceIdProvider> { DataStoreDeviceIdProvider(get()) }
        single<ConnectionManager> { NearbyConnectionManager(get(), get()) }
        single<LobbyManager> { NearbyLobbyManager(get(), get(), get(), get()) }
        single<LocalPreferencesRepository> {
            object : LocalPreferencesRepository {
                override val preferences = MutableStateFlow(
                    LocalPreferences(
                        userProfile = UserProfile(
                            displayName = get<DeviceNameProvider>().getDeviceName(),
                            displayColor = ProfileColor.Color1
                        )
                    )
                )

                override suspend fun updatePreferences(updated: LocalPreferences) {}
            }
        }

        single { MainViewModel(get()) }
        single { RoomViewModel(get()) }
    }
}