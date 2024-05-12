package fr.outadoc.pictochat.ui.room

import androidx.compose.ui.geometry.Size

object InputConfig {

    val CanvasSize = Size(
        width = 230f,
        height = 81f
    )
    val CanvasRatio = CanvasSize.width / CanvasSize.height
    val MaxLines = 5
}
