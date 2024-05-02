package fr.outadoc.pictochat.domain

data class RemoteDevice(
    val endpointId: String,
    val deviceId: String,
) {
    fun isSameDevice(other: RemoteDevice): Boolean {
        return deviceId == other.deviceId || endpointId == other.endpointId
    }
}