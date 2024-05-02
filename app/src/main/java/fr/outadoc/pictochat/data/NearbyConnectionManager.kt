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
import fr.outadoc.pictochat.domain.ConnectionManager
import fr.outadoc.pictochat.domain.RemoteDevice
import fr.outadoc.pictochat.preferences.DeviceIdProvider
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
    private val deviceIdProvider: DeviceIdProvider,
) : ConnectionManager {

    private var _state: MutableStateFlow<ConnectionManager.State> =
        MutableStateFlow(ConnectionManager.State())
    override val state = _state.asStateFlow()

    private val _payloadFlow = MutableSharedFlow<ReceivedPayload>(extraBufferCapacity = 32)
    override val payloadFlow = _payloadFlow.asSharedFlow()

    private var connectionJob: Job? = null

    private var connectionsClient: ConnectionsClient =
        Nearby.getConnectionsClient(applicationContext)

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            val device = RemoteDevice(
                endpointId = endpointId,
                deviceId = connectionInfo.endpointName
            )

            _state.update { state ->
                if (state.connectedEndpoints.contains(device)
                    || state.connectingEndpoints.contains(device)
                ) {
                    // We're already connected to the device with that device ID
                    Log.d(TAG, "Rejecting connection to $endpointId")
                    connectionsClient.rejectConnection(endpointId)
                    return
                }

                state.copy(
                    connectingEndpoints = state.connectingEndpoints.add(device)
                )
            }

            Log.d(TAG, "Accepting connection to $endpointId")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "Connected successfully to $endpointId")
                    _state.update { state ->
                        val device = state.connectingEndpoints.first { it.endpointId == endpointId }
                        state.copy(
                            connectedEndpoints = state.connectedEndpoints.add(device),
                            connectingEndpoints = state.connectingEndpoints.remove(device)
                        )
                    }
                }

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG, "Connection rejected for $endpointId")
                    _state.update { state ->
                        state.copy(
                            connectedEndpoints = state.connectedEndpoints.removeAll { it.endpointId == endpointId },
                            connectingEndpoints = state.connectingEndpoints.removeAll { it.endpointId == endpointId }
                        )
                    }
                }

                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.d(TAG, "Connection error for $endpointId")
                    _state.update { state ->
                        state.copy(
                            connectedEndpoints = state.connectedEndpoints.removeAll { it.endpointId == endpointId },
                            connectingEndpoints = state.connectingEndpoints.removeAll { it.endpointId == endpointId }
                        )
                    }
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "onDisconnected: $endpointId")

            _state.update { state ->
                state.copy(
                    connectedEndpoints = state.connectedEndpoints.removeAll { it.endpointId == endpointId },
                    connectingEndpoints = state.connectingEndpoints.removeAll { it.endpointId == endpointId }
                )
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val device = RemoteDevice(
                endpointId = endpointId,
                deviceId = info.endpointName
            )

            val state = _state.value
            if (state.connectedEndpoints.contains(device)
                || state.connectingEndpoints.contains(device)
            ) {
                // We're already connected to the device with that device ID
                Log.d(TAG, "Ignoring endpoint $device")
                return
            }

            Log.d(TAG, "Requesting connection to $device")

            connectionsClient.requestConnection(
                deviceIdProvider.deviceId,
                endpointId,
                connectionLifecycleCallback
            )
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "onEndpointLost: $endpointId")
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val payloadCallback: PayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val proto = ProtoBuf.decodeFromByteArray<ChatPayload>(payload.asBytes()!!)
            Log.d(TAG, "onPayloadReceived: $endpointId, payload: $proto")
            _payloadFlow.tryEmit(
                ReceivedPayload(
                    sender = _state.value.connectedEndpoints.first { it.endpointId == endpointId },
                    data = proto
                )
            )
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            Log.d(TAG, "onPayloadTransferUpdate: $endpointId, transfer: ${update.status}")
        }
    }

    override suspend fun connect() {
        connectionJob?.cancel()
        connectionJob = coroutineScope {
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

            launch(Dispatchers.IO) {
                Log.d(TAG, "startAdvertising")
                connectionsClient
                    .startAdvertising(
                        deviceIdProvider.deviceId,
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
    override suspend fun sendPayload(endpointId: String, payload: ChatPayload) {
        val protoBytes = ProtoBuf.encodeToByteArray(payload)
        Log.d(TAG, "Sending payload to $endpointId: $payload")

        connectionsClient
            .sendPayload(endpointId, Payload.fromBytes(protoBytes))
            .await()
    }

    override fun close() {
        connectionJob?.cancel()

        connectionsClient.apply {
            stopDiscovery()
            stopAdvertising()
            stopAllEndpoints()
        }
    }

    companion object {
        private val STRATEGY = Strategy.P2P_CLUSTER
        private const val PICTOCHAT_SERVICE_ID = "fr.outadoc.pictochat"
        private const val TAG = "NearbyConnectionManager"
    }
}