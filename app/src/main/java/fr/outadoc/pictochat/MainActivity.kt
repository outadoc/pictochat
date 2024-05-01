package fr.outadoc.pictochat

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.outadoc.pictochat.ui.theme.PictoChatTheme
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
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
                            single<NearbyConnectionManager> {
                                NearbyConnectionManagerImpl(
                                    get(),
                                    get()
                                )
                            }
                            single { MainViewModel(get()) }
                        }
                    )
                }
            ) {
                PictoChatTheme {
                    val viewModel = koinInject<MainViewModel>()
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        LaunchedEffect(Any()) {
                            viewModel.start()
                        }

                        Greeting(
                            name = "Android",
                            modifier = Modifier
                                .padding(innerPadding)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
