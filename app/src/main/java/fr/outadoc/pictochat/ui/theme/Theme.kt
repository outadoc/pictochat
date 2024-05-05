package fr.outadoc.pictochat.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun PictoChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    favoriteColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        favoriteColor != null -> {
            if (darkTheme) {
                darkSchemeFromColor(favoriteColor)
            } else {
                lightSchemeFromColor(favoriteColor)
            }
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> {
            darkColorScheme()
        }

        else -> {
            lightColorScheme()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}