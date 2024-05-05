package fr.outadoc.pictochat.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import fr.outadoc.pictochat.domain.ProfileColor

@Composable
fun ProfileColor.toColor(): Color {
    return if (MaterialTheme.colorScheme.isLight()) {
        when (this) {
            ProfileColor.Color1 -> Color(0xFF61829a)
            ProfileColor.Color2 -> Color(0xFFba4900)
            ProfileColor.Color3 -> Color(0xFFfb0018)
            ProfileColor.Color4 -> Color(0xFFfb8afb)
            ProfileColor.Color5 -> Color(0xFFfb9200)
            ProfileColor.Color6 -> Color(0xFFf3e300)
            ProfileColor.Color7 -> Color(0xFFaafb00)
            ProfileColor.Color8 -> Color(0xFF00fb00)
            ProfileColor.Color9 -> Color(0xFF00a238)
            ProfileColor.Color10 -> Color(0xFF49db8a)
            ProfileColor.Color11 -> Color(0xFF30baf3)
            ProfileColor.Color12 -> Color(0xFF0059f3)
            ProfileColor.Color13 -> Color(0xFF000092)
            ProfileColor.Color14 -> Color(0xFF8a00d3)
            ProfileColor.Color15 -> Color(0xFFd300eb)
            ProfileColor.Color16 -> Color(0xFFfb0092)
        }
    } else {
        when (this) {
            ProfileColor.Color1 -> Color(0xFF61829a)
            ProfileColor.Color2 -> Color(0xFFba4900)
            ProfileColor.Color3 -> Color(0xFFfb0018)
            ProfileColor.Color4 -> Color(0xFFfb8afb)
            ProfileColor.Color5 -> Color(0xFFfb9200)
            ProfileColor.Color6 -> Color(0xFFf3e300)
            ProfileColor.Color7 -> Color(0xFFaafb00)
            ProfileColor.Color8 -> Color(0xFF00fb00)
            ProfileColor.Color9 -> Color(0xFF00a238)
            ProfileColor.Color10 -> Color(0xFF49db8a)
            ProfileColor.Color11 -> Color(0xFF30baf3)
            ProfileColor.Color12 -> Color(0xFF0059f3)
            ProfileColor.Color13 -> Color(0xFF000092)
            ProfileColor.Color14 -> Color(0xFF8a00d3)
            ProfileColor.Color15 -> Color(0xFFd300eb)
            ProfileColor.Color16 -> Color(0xFFfb0092)
        }
    }
}

@Composable
fun ColorScheme.isLight() = this.background.luminance() > 0.5

@PreviewLightDark
@Composable
private fun ProfileColorPreview() {
    PictoChatTheme {
        Column {
            ProfileColor.entries.forEach { color ->
                Surface(color = color.toColor()) {
                    Text(
                        modifier = Modifier.padding(4.dp),
                        text = color.id.toString().padStart(2, '0')
                    )
                }
            }
        }
    }
}
