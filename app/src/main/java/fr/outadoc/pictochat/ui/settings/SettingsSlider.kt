package fr.outadoc.pictochat.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.outadoc.pictochat.ui.theme.PictoChatTheme

@Composable
fun SettingsSlider(
    modifier: Modifier = Modifier,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange = 0..100,
    steps: Int = 0,
    valueContent: @Composable (Int) -> Unit = {},
    title: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.titleMedium,
    ) {
        Column(modifier = modifier) {
            title()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    modifier = Modifier.weight(1f, fill = true),
                    value = value.toFloat(),
                    onValueChange = { value -> onValueChange(value.toInt()) },
                    valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
                    steps = if (steps == 0) 0 else valueRange.last / steps,
                )

                valueContent(value)
            }
        }
    }
}

@Preview
@Composable
private fun SettingsSliderPreview() {
    PictoChatTheme {
        Column(modifier = Modifier.width(256.dp)) {
            SettingsSlider(
                modifier = Modifier.fillMaxWidth(),
                value = 30,
                onValueChange = {},
            ) {
                Text("Lorem ipsum")
            }

            SettingsSlider(
                modifier = Modifier.fillMaxWidth(),
                value = 300,
                valueRange = 10..1000,
                steps = 10,
                onValueChange = {},
            ) {
                Text("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque at arcu at neque tempus sollicitudin.")
            }

            SettingsSlider(
                modifier = Modifier.fillMaxWidth(),
                value = 1,
                onValueChange = {},
            ) {
                Text("Lorem ipsum")
            }
        }
    }
}
