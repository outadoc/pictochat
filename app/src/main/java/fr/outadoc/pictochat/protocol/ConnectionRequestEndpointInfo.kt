package fr.outadoc.pictochat.protocol

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ConnectionRequestEndpointInfo(
    @ProtoNumber(1)
    val sourceDeviceId: String,
    @ProtoNumber(2)
    val targetSessionId: String,
)
