package fr.outadoc.pictochat.domain

import android.net.wifi.aware.PeerHandle
import fr.outadoc.pictochat.preferences.DeviceId

data class RemoteDevice(
    val endpointId: PeerHandle,
    val deviceId: DeviceId,
)
