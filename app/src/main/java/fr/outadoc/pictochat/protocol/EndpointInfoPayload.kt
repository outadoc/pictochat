package fr.outadoc.pictochat.protocol

import fr.outadoc.pictochat.preferences.DeviceId
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class EndpointInfoPayload(
    @ProtoNumber(1)
    val deviceId: DeviceId,
)
