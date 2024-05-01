package fr.outadoc.pictochat.domain

data class RemoteDevice(
    val endpointId: String,
    val deviceId: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RemoteDevice

        // If either the endpoint ID or the device ID matches, we consider the devices equal
        return endpointId == other.endpointId || deviceId == other.deviceId
    }

    override fun hashCode(): Int {
        var result = endpointId.hashCode()
        result = 31 * result + deviceId.hashCode()
        return result
    }
}