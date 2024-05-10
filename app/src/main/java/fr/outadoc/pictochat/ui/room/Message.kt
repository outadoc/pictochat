package fr.outadoc.pictochat.ui.room

import androidx.compose.runtime.Stable

@Stable
data class Message(
    val contentDescription: String,
    val bitmap: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message

        if (contentDescription != other.contentDescription) return false
        if (!bitmap.contentEquals(other.bitmap)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contentDescription.hashCode()
        result = 31 * result + bitmap.contentHashCode()
        return result
    }
}