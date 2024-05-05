package fr.outadoc.pictochat.ui.navigation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import fr.outadoc.pictochat.preferences.LocalPreferencesRepository
import fr.outadoc.pictochat.ui.theme.PictoChatTheme
import fr.outadoc.pictochat.ui.theme.toColor
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KoinContext {
                val prefsRepository = koinInject<LocalPreferencesRepository>()
                val userPrefs by prefsRepository.preferences.collectAsState(null)
                val favoriteColor = userPrefs?.userProfile?.displayColor?.toColor()

                PictoChatTheme(favoriteColor = favoriteColor) {
                    MainNavigation()
                }
            }
        }
    }
}
