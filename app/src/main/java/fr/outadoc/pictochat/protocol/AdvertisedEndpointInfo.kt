package fr.outadoc.pictochat.protocol

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AdvertisedEndpointInfo(
    @ProtoNumber(1)
    val sessionId: String,
)
