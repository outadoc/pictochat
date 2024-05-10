package fr.outadoc.pictochat.domain

data class Message(
    val contentDescription: String,
    val drawing: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message

        if (contentDescription != other.contentDescription) return false
        if (!drawing.contentEquals(other.drawing)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contentDescription.hashCode()
        result = 31 * result + drawing.contentHashCode()
        return result
    }
}