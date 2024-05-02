package fr.outadoc.pictochat

import android.app.Application
import android.content.Context
import fr.outadoc.pictochat.data.NearbyConnectionManager
import fr.outadoc.pictochat.data.NearbyLobbyManager
import fr.outadoc.pictochat.domain.ConnectionManager
import fr.outadoc.pictochat.domain.LobbyManager
import fr.outadoc.pictochat.preferences.AndroidDeviceNameProvider
import fr.outadoc.pictochat.preferences.DataStoreDeviceIdProvider
import fr.outadoc.pictochat.preferences.DeviceIdProvider
import fr.outadoc.pictochat.preferences.DeviceNameProvider
import fr.outadoc.pictochat.preferences.LocalPreferences
import fr.outadoc.pictochat.preferences.LocalPreferencesProvider
import fr.outadoc.pictochat.preferences.UserProfile
import fr.outadoc.pictochat.ui.navigation.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.context.startKoin
import org.koin.dsl.module

class Application : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            modules(commonModule)
        }
    }

    private val commonModule = module {
        single<Context> { applicationContext }
        single<DeviceNameProvider> { AndroidDeviceNameProvider(get()) }
        single<DeviceIdProvider> { DataStoreDeviceIdProvider(get()) }
        single<ConnectionManager> { NearbyConnectionManager(get(), get()) }
        single<LobbyManager> { NearbyLobbyManager(get(), get()) }
        single<LocalPreferencesProvider> {
            object : LocalPreferencesProvider {
                override val preferences = MutableStateFlow(
                    LocalPreferences(
                        userProfile = UserProfile(
                            displayName = get<DeviceNameProvider>().getDeviceName(),
                            displayColor = 0xff0000
                        )
                    )
                )
            }
        }
        single { MainViewModel(get()) }
    }
}