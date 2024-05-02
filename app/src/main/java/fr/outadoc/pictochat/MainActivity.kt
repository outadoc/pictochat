package fr.outadoc.pictochat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.outadoc.pictochat.ui.theme.PictoChatTheme
import org.koin.compose.KoinContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KoinContext {
                PictoChatTheme {
                    HomeScreen()
                }
            }
        }
    }
}
