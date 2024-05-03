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
import fr.outadoc.pictochat.preferences.DeviceId
import fr.outadoc.pictochat.preferences.DeviceIdProvider
import fr.outadoc.pictochat.protocol.ChatPayload
import fr.outadoc.pictochat.protocol.EndpointInfoPayload
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class, ExperimentalStdlibApi::class)
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
            val decoded = try {
                ProtoBuf.decodeFromByteArray<EndpointInfoPayload>(
                    connectionInfo.endpointInfo
                )
            } catch (e: SerializationException) {
                Log.d(TAG, "Rejected connection to $endpointId, could not decode payload")
                connectionsClient.rejectConnection(endpointId)
                return
            }

            val device = RemoteDevice(
                endpointId = endpointId,
                deviceId = DeviceId(decoded.deviceId)
            )

            _state.update { state ->
                val connectedDevice = state.connectedEndpoints.firstOrNull { connectedDevice ->
                    connectedDevice.deviceId == device.deviceId
                }

                if (connectedDevice != null) {
                    Log.w(
                        TAG,
                        "$device is already known as $connectedDevice, closing existing connection"
                    )
                    connectionsClient.disconnectFromEndpoint(connectedDevice.endpointId)
                }

                state.copy(
                    connectedEndpoints = connectedDevice?.let {
                        state.connectedEndpoints.remove(connectedDevice)
                    } ?: state.connectedEndpoints,
                    approvedEndpoints = state.approvedEndpoints.add(device)
                )
            }

            Log.d(TAG, "Accepting connection to $device, got payload $decoded")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            _state.update { state ->
                val device = state.approvedEndpoints.firstOrNull { it.endpointId == endpointId }

                if (device == null) {
                    Log.d(TAG, "Ignoring connection result for unknown endpoint $endpointId")
                    return@update state
                }

                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        Log.d(TAG, "Connected successfully to $endpointId")
                        state.copy(
                            connectedEndpoints = state.connectedEndpoints.add(device),
                            approvedEndpoints = state.approvedEndpoints.remove(device)
                        )
                    }

                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                        Log.d(TAG, "Connection rejected for $endpointId")
                        state.copy(
                            connectedEndpoints = state.connectedEndpoints.remove(device),
                            approvedEndpoints = state.approvedEndpoints.remove(device)
                        )
                    }

                    ConnectionsStatusCodes.STATUS_ERROR -> {
                        Log.d(TAG, "Connection error for $endpointId")
                        state.copy(
                            connectedEndpoints = state.connectedEndpoints.remove(device),
                            approvedEndpoints = state.approvedEndpoints.remove(device)
                        )
                    }

                    else -> {
                        Log.d(TAG, "Connection result for $endpointId: ${result.status}")
                        state
                    }
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            _state.update { state ->
                val device = state.approvedEndpoints.firstOrNull { it.endpointId == endpointId }

                if (device == null) {
                    Log.d(TAG, "Ignoring connection result for unknown endpoint $endpointId")
                    return@update state
                }

                Log.d(TAG, "onDisconnected: $device")

                state.copy(
                    connectedEndpoints = state.connectedEndpoints.remove(device),
                    approvedEndpoints = state.approvedEndpoints.remove(device)
                )
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val decoded = try {
                ProtoBuf.decodeFromByteArray<EndpointInfoPayload>(info.endpointInfo)
            } catch (e: SerializationException) {
                Log.e(
                    TAG,
                    "Ignoring $endpointId, could not decode payload: ${info.endpointInfo.toHexString()}",
                    e
                )
                return
            }

            val device = RemoteDevice(
                endpointId = endpointId,
                deviceId = DeviceId(decoded.deviceId)
            )

            if (_state.value.connectedEndpoints.any { connectedDevice -> connectedDevice.deviceId == device.deviceId }) {
                Log.w(TAG, "Ignoring $device, already connected")
                return
            }

            val payload = EndpointInfoPayload(
                deviceId = deviceIdProvider.deviceId.value,
            )

            Log.i(
                TAG,
                "Requesting connection to $device: got $decoded, sending $payload}"
            )

            connectionsClient.requestConnection(
                ProtoBuf.encodeToByteArray(payload),
                endpointId,
                connectionLifecycleCallback
            )
        }

        override fun onEndpointLost(endpointId: String) {
            Log.w(TAG, "onEndpointLost: $endpointId")
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
            Log.d(TAG, "connect: deviceId=${deviceIdProvider.deviceId}")

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
                val endpointInfo = EndpointInfoPayload(
                    deviceId = deviceIdProvider.deviceId.value
                )

                Log.d(TAG, "startAdvertising: $endpointInfo")

                connectionsClient
                    .startAdvertising(
                        ProtoBuf.encodeToByteArray(endpointInfo),
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
        Log.d(TAG, "Closing all connections")

        connectionJob?.cancel()

        _state.updateAndGet { state ->
            state.copy(
                connectedEndpoints = persistentSetOf(),
                approvedEndpoints = persistentSetOf()
            )
        }

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