package fr.outadoc.pictochat.domain

import fr.outadoc.pictochat.ui.room.Message

data class Message(
    val text: String,
    val bitmap: IntArray,
    val bitmapHeight: Int,
    val bitmapWidth: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message

        if (text != other.message) return false
        if (!bitmap.contentEquals(other.bitmap)) return false
        if (bitmapHeight != other.bitmapHeight) return false
        if (bitmapWidth != other.bitmapWidth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + bitmap.contentHashCode()
        result = 31 * result + bitmapHeight
        result = 31 * result + bitmapWidth
        return result
    }
}