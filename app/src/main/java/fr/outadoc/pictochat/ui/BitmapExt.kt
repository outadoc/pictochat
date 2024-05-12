package fr.outadoc.pictochat.ui

import android.graphics.Bitmap
import android.graphics.Color

fun Bitmap.trimTransparentVerticalPixels(paddingPx: Int): Bitmap {
    val originalHeight = height
    val originalWidth = width

    // Check top pixels
    var startHeight = 0
    for (y in 0 until originalHeight) {
        if (startHeight == 0) {
            for (x in 0 until originalWidth) {
                if (getPixel(x, y) != Color.TRANSPARENT) {
                    startHeight = y
                    break
                }
            }
        } else {
            break
        }
    }

    // Check bottom pixels
    var endHeight = 0
    for (y in originalHeight - 1 downTo 0) {
        if (endHeight == 0) {
            for (x in 0 until originalWidth) {
                if (getPixel(x, y) != Color.TRANSPARENT) {
                    endHeight = y
                    break
                }
            }
        } else {
            break
        }
    }

    // Ensure we keep paddingPx transparent pixels on the top and bottom

    val actualStartHeight = (startHeight - paddingPx).coerceAtLeast(0)
    val actualEndHeight = (endHeight + paddingPx).coerceAtMost(originalHeight)

    return Bitmap.createBitmap(
        /* source = */ this,
        /* x = */ 0,
        /* y = */ actualStartHeight,
        /* width = */ originalWidth,
        /* height = */ actualEndHeight - actualStartHeight
    )
}
