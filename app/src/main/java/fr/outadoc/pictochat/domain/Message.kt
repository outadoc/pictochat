package fr.outadoc.pictochat.domain

import fr.outadoc.pictochat.ui.room.Message

data class Message(
    val message: String,
    val bitmap: IntArray,
    val bitmapHeight: Int,
    val bitmapWidth: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message

        if (message != other.message) return false
        if (!bitmap.contentEquals(other.bitmap)) return false
        if (bitmapHeight != other.bitmapHeight) return false
        if (bitmapWidth != other.bitmapWidth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = message.hashCode()
        result = 31 * result + bitmap.contentHashCode()
        result = 31 * result + bitmapHeight
        result = 31 * result + bitmapWidth
        return result
    }
}