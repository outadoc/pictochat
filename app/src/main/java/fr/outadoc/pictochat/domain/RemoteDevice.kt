package fr.outadoc.pictochat.domain

import fr.outadoc.pictochat.preferences.DeviceId

data class RemoteDevice(
    val endpointId: String,
    val deviceId: DeviceId
)
