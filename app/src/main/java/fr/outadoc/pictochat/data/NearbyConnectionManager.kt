package fr.outadoc.pictochat.data

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import fr.outadoc.pictochat.LocalPreferencesProvider
import fr.outadoc.pictochat.domain.ConnectionManager
import fr.outadoc.pictochat.protocol.ChatPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

class NearbyConnectionManager(
    applicationContext: Context,
    private val localPreferencesProvider: LocalPreferencesProvider,
) : ConnectionManager {

    private var _state: MutableStateFlow<ConnectionManager.State> =
        MutableStateFlow(ConnectionManager.State())
    override val state = _state.asStateFlow()

    private val _payloadFlow = MutableSharedFlow<ChatPayload>()
    override val payloadFlow = _payloadFlow.asSharedFlow()

    private var discoveryJob: Job? = null
    private var advertisingJob: Job? = null

    private var connectionsClient: ConnectionsClient =
        Nearby.getConnectionsClient(applicationContext)

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(TAG, "onConnectionInitiated: $endpointId, ${connectionInfo.endpointName}")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            Log.d(TAG, "onConnectionResult: $endpointId")
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "Connection successful")
                    _state.update { state ->
                        state.copy(
                            connectedEndpoints = state.connectedEndpoints + endpointId
                        )
                    }
                }

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG, "Connection rejected")
                }

                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.d(TAG, "Connection error")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "onDisconnected: $endpointId")

            _state.update { state ->
                state.copy(
                    connectedEndpoints = state.connectedEndpoints - endpointId
                )
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "onEndpointFound: $endpointId, ${info.endpointName}")

            connectionsClient.requestConnection(
                localPreferencesProvider.preferences.value.userProfile.displayName,
                endpointId,
                connectionLifecycleCallback
            )
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "onEndpointLost: $endpointId")

            _state.update { state ->
                state.copy(
                    connectedEndpoints = state.connectedEndpoints - endpointId
                )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val payloadCallback: PayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val proto = ProtoBuf.decodeFromByteArray<ChatPayload>(payload.asBytes()!!)
            Log.d(TAG, "onPayloadReceived: $endpointId, payload: $proto")
            _payloadFlow.tryEmit(proto)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            Log.d(TAG, "onPayloadTransferUpdate: $endpointId, transfer: ${update.status}")
        }
    }

    override suspend fun startDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = coroutineScope {
            launch(Dispatchers.IO) {
                Log.d(TAG, "startDiscovery")
                connectionsClient
                    .startDiscovery(
                        PICTOCHAT_SERVICE_ID,
                        endpointDiscoveryCallback,
                        DiscoveryOptions.Builder()
                            .setStrategy(STRATEGY)
                            .build()
                    )
                    .await()
            }
        }
    }

    override suspend fun startAdvertising() {
        advertisingJob?.cancel()
        advertisingJob = coroutineScope {
            launch(Dispatchers.IO) {
                Log.d(TAG, "startAdvertising")
                connectionsClient
                    .startAdvertising(
                        localPreferencesProvider.preferences.value.userProfile.displayName,
                        PICTOCHAT_SERVICE_ID,
                        connectionLifecycleCallback,
                        AdvertisingOptions.Builder()
                            .setStrategy(STRATEGY)
                            .build()
                    )
                    .await()
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun sendPayload(endpointId: String, payload: ChatPayload) {
        val protoBytes = ProtoBuf.encodeToByteArray(payload)
        Log.d(TAG, "Sending payload to $endpointId: $payload")
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(protoBytes))
    }

    override fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        discoveryJob?.cancel()
    }

    override fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        advertisingJob?.cancel()
    }

    override fun close() {
        stopDiscovery()
        stopAdvertising()
    }

    companion object {
        private val STRATEGY = Strategy.P2P_CLUSTER
        private const val PICTOCHAT_SERVICE_ID = "fr.outadoc.pictochat"
        private const val TAG = "NearbyConnectionManager"
    }
}