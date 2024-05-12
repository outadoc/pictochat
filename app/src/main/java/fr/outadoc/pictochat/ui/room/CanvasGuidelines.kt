package fr.outadoc.pictochat.ui.room

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

@Composable
fun CanvasGuidelines(
    modifier: Modifier = Modifier,
    guidelineColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier
    ) {
        // Draw four horizontal lines at equal intervals
        for (i in 1..4) {
            val y = size.height / 5 * i
            drawLine(
                color = guidelineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }
    }
}
