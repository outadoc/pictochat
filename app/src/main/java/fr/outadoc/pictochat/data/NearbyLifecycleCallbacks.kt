package fr.outadoc.pictochat.data

import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadTransferUpdate

interface NearbyLifecycleCallbacks {
    suspend fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo)
    suspend fun onConnectionResult(endpointId: String, result: ConnectionResolution)
    suspend fun onDisconnected(endpointId: String)
    suspend fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo)
    suspend fun onEndpointLost(endpointId: String)
    suspend fun onPayloadReceived(endpointId: String, payload: Payload)
    suspend fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate)
}
