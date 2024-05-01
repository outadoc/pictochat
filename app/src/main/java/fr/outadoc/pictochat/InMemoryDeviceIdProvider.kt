package fr.outadoc.pictochat

import java.util.UUID

class InMemoryDeviceIdProvider : DeviceIdProvider {
    override val deviceId: String = UUID.randomUUID().toString()
}
