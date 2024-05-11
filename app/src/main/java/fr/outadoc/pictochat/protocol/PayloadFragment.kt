package fr.outadoc.pictochat.protocol

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PayloadFragment(
    @ProtoNumber(1)
    val id: Int,
    @ProtoNumber(2)
    val order: Int,
    @ProtoNumber(3)
    val totalCount: Int,
    @ProtoNumber(4)
    val totalBytes: Int,
    @ProtoNumber(5)
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PayloadFragment

        if (id != other.id) return false
        if (order != other.order) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + order
        result = 31 * result + data.contentHashCode()
        return result
    }
}
