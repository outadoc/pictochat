package fr.outadoc.pictochat

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.outadoc.pictochat.data.NearbyConnectionManager
import fr.outadoc.pictochat.data.NearbyLobbyManager
import fr.outadoc.pictochat.domain.ConnectionManager
import fr.outadoc.pictochat.domain.LobbyManager
import fr.outadoc.pictochat.ui.theme.PictoChatTheme
import org.koin.compose.KoinApplication
import org.koin.dsl.module

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KoinApplication(
                application = {
                    modules(
                        module {
                            single<Context> { applicationContext }
                            single<DeviceNameProvider> { AndroidDeviceNameProvider(get()) }
                            single<ConnectionManager> { NearbyConnectionManager(get(), get()) }
                            single<LobbyManager> { NearbyLobbyManager(get()) }
                            single { MainViewModel(get()) }
                        }
                    )
                }
            ) {
                PictoChatTheme {
                    HomeScreen()
                }
            }
        }
    }
}
