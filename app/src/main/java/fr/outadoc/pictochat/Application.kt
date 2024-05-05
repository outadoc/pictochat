package fr.outadoc.pictochat

import android.app.Application
import android.content.Context
import android.os.StrictMode
import fr.outadoc.pictochat.data.NearbyConnectionManager
import fr.outadoc.pictochat.data.NearbyLobbyManager
import fr.outadoc.pictochat.domain.ConnectionManager
import fr.outadoc.pictochat.domain.LobbyManager
import fr.outadoc.pictochat.preferences.AndroidDeviceNameProvider
import fr.outadoc.pictochat.preferences.DataStoreDeviceIdProvider
import fr.outadoc.pictochat.preferences.DataStoreLocalPreferencesRepository
import fr.outadoc.pictochat.preferences.DeviceIdProvider
import fr.outadoc.pictochat.preferences.DeviceNameProvider
import fr.outadoc.pictochat.preferences.LocalPreferencesRepository
import fr.outadoc.pictochat.ui.navigation.MainViewModel
import fr.outadoc.pictochat.ui.room.RoomViewModel
import fr.outadoc.pictochat.ui.settings.SettingsViewModel
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
        single<LocalPreferencesRepository> { DataStoreLocalPreferencesRepository(get(), get()) }

        single { MainViewModel(get()) }
        single { RoomViewModel(get()) }
        single { SettingsViewModel(get()) }
    }
}