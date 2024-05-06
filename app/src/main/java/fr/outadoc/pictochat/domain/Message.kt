package fr.outadoc.pictochat.domain

data class Message(
    val contentDescription: String,
    val bitmap: IntArray,
    val bitmapHeight: Int,
    val bitmapWidth: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message

        if (contentDescription != other.contentDescription) return false
        if (!bitmap.contentEquals(other.bitmap)) return false
        if (bitmapHeight != other.bitmapHeight) return false
        if (bitmapWidth != other.bitmapWidth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contentDescription.hashCode()
        result = 31 * result + bitmap.contentHashCode()
        result = 31 * result + bitmapHeight
        result = 31 * result + bitmapWidth
        return result
    }
}