package fr.outadoc.pictochat.data

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.ApiException
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalSerializationApi::class, ExperimentalStdlibApi::class)
class NearbyConnectionManager(
    applicationContext: Context,
    private val deviceIdProvider: DeviceIdProvider,
) : ConnectionManager, NearbyLifecycleCallbacks {

    private var _state: MutableStateFlow<ConnectionManager.State> =
        MutableStateFlow(ConnectionManager.State())
    override val state = _state.asStateFlow()

    private val stateLock = Mutex()

    private val _payloadFlow = MutableSharedFlow<ReceivedPayload>(extraBufferCapacity = 32)
    override val payloadFlow = _payloadFlow.asSharedFlow()

    private val connectionsClient: ConnectionsClient =
        Nearby.getConnectionsClient(applicationContext)

    private var _connectionScope: CoroutineScope? = null

    override suspend fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
        val decoded = try {
            ProtoBuf.decodeFromByteArray<EndpointInfoPayload>(
                connectionInfo.endpointInfo
            )
        } catch (e: SerializationException) {
            Log.d(TAG, "Rejected connection to $endpointId, could not decode payload")

            wrap(label = "rejectConnection") {
                connectionsClient
                    .rejectConnection(endpointId)
                    .await()
            }

            return
        }

        val device = RemoteDevice(
            endpointId = endpointId,
            deviceId = DeviceId(decoded.deviceId)
        )

        stateLock.withLock {
            delay(1.seconds)

            _state.update { state ->
                Log.d(TAG, "onConnectionInitiated: Current state: $state")

                val connectedDevice = state.connectedEndpoints
                    .firstOrNull { connectedDevice ->
                        connectedDevice.deviceId == device.deviceId
                    }

                if (connectedDevice != null) {
                    Log.w(
                        TAG,
                        "onConnectionInitiated: $device is already known as $connectedDevice, closing existing connection"
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

            Log.d(
                TAG,
                "onConnectionInitiated: Accepting connection to $device, got payload $decoded"
            )

            wrap(label = "acceptConnection") {
                connectionsClient
                    .acceptConnection(endpointId, payloadCallbackDelegate)
                    .await()
            }
        }
    }

    override suspend fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
        stateLock.withLock {
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

                    ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT -> {
                        Log.d(TAG, "Already connected to $endpointId")
                        state
                    }

                    else -> {
                        Log.d(
                            TAG,
                            "Connection result for $endpointId: ${
                                ConnectionsStatusCodes.getStatusCodeString(result.status.statusCode)
                            }"
                        )
                        state.copy(
                            connectedEndpoints = state.connectedEndpoints.remove(device),
                            approvedEndpoints = state.approvedEndpoints.remove(device)
                        )
                    }
                }
            }
        }
    }

    override suspend fun onDisconnected(endpointId: String) {
        stateLock.withLock {
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

    override suspend fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
        stateLock.withLock {
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

            val discoveredDevice = RemoteDevice(
                endpointId = endpointId,
                deviceId = DeviceId(decoded.deviceId)
            )

            if (_state.value.connectedEndpoints.any { connectedDevice -> connectedDevice.deviceId == discoveredDevice.deviceId }) {
                Log.w(TAG, "Ignoring $discoveredDevice, already connected")
                return
            }

            val myDeviceId = deviceIdProvider.deviceId.value

            if (myDeviceId.hashCode() > discoveredDevice.deviceId.hashCode()) {
                Log.w(
                    TAG,
                    "Ignoring $discoveredDevice, our device ID is greater; waiting for connection request"
                )
                return
            }

            val payload = EndpointInfoPayload(deviceId = myDeviceId)

            Log.i(
                TAG,
                "Requesting connection to $discoveredDevice: got $decoded, sending $payload}"
            )

            // Add some delay so that the other device is ready
            delay(1.seconds)

            wrap(label = "requestConnection") {
                connectionsClient
                    .requestConnection(
                        ProtoBuf.encodeToByteArray(payload),
                        endpointId,
                        connectionLifecycleCallbackDelegate
                    )
                    .await()
            }
        }
    }

    override suspend fun onEndpointLost(endpointId: String) {
    }

    override suspend fun onPayloadReceived(endpointId: String, payload: Payload) {
        val proto = ProtoBuf.decodeFromByteArray<ChatPayload>(payload.asBytes()!!)
        Log.d(TAG, "onPayloadReceived: $endpointId, payload: $proto")
        stateLock.withLock {
            _payloadFlow.tryEmit(
                ReceivedPayload(
                    sender = _state.value.connectedEndpoints.first { it.endpointId == endpointId },
                    data = proto
                )
            )
        }
    }

    override suspend fun onPayloadTransferUpdate(
        endpointId: String,
        update: PayloadTransferUpdate,
    ) {
    }

    override suspend fun connect() {
        coroutineScope {
            _connectionScope?.cancel()
            _connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            launch {
                Log.d(TAG, "connect: deviceId=${deviceIdProvider.deviceId}")

                launch(Dispatchers.IO) {
                    Log.d(TAG, "startDiscovery")

                    wrap(label = "startDiscovery") {
                        connectionsClient
                            .startDiscovery(
                                PICTOCHAT_SERVICE_ID,
                                endpointDiscoveryCallbackDelegate,
                                DiscoveryOptions.Builder()
                                    .setStrategy(STRATEGY)
                                    .build()
                            )
                            .await()
                    }
                }

                launch(Dispatchers.IO) {
                    val endpointInfo = EndpointInfoPayload(
                        deviceId = deviceIdProvider.deviceId.value
                    )

                    Log.d(TAG, "startAdvertising: $endpointInfo")

                    wrap(label = "startAdvertising") {
                        connectionsClient
                            .startAdvertising(
                                ProtoBuf.encodeToByteArray(endpointInfo),
                                PICTOCHAT_SERVICE_ID,
                                connectionLifecycleCallbackDelegate,
                                AdvertisingOptions.Builder()
                                    .setStrategy(STRATEGY)
                                    .build()
                            )
                            .await()
                    }

                    _state.update { state ->
                        state.copy(
                            isOnline = true,
                            connectedEndpoints = persistentSetOf(),
                            approvedEndpoints = persistentSetOf()
                        )
                    }

                    try {
                        awaitCancellation()
                    } catch (e: Exception) {
                        Log.d(TAG, "Connection cancelled")
                        close()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun sendPayload(endpointId: String, payload: ChatPayload) {
        val protoBytes = ProtoBuf.encodeToByteArray(payload)
        Log.d(TAG, "Sending payload to $endpointId: $payload")

        wrap(label = "sendPayload") {
            connectionsClient
                .sendPayload(endpointId, Payload.fromBytes(protoBytes))
                .await()
        }
    }

    override fun close() {
        Log.d(TAG, "Closing all connections")

        connectionsClient.apply {
            stopDiscovery()
            stopAdvertising()
            stopAllEndpoints()
        }

        _connectionScope?.cancel()

        _state.updateAndGet { state ->
            state.copy(
                isOnline = false,
                connectedEndpoints = persistentSetOf(),
                approvedEndpoints = persistentSetOf()
            )
        }
    }

    private suspend fun <T> wrap(
        label: String,
        block: suspend () -> T,
    ) {
        try {
            retry(label = label) {
                try {
                    block()
                } catch (e: Exception) {
                    when (e) {
                        is ApiException -> {
                            // Errors that we don't need to retry
                            when (e.statusCode) {
                                ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT -> {
                                    Log.w(TAG, "Already connected to endpoint", e)
                                }

                                ConnectionsStatusCodes.STATUS_ENDPOINT_UNKNOWN -> {
                                    Log.w(TAG, "Endpoint unknown", e)
                                }

                                ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING -> {
                                    Log.w(TAG, "Already discovering", e)
                                }

                                ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING -> {
                                    Log.w(TAG, "Already advertising", e)
                                }

                                else -> throw e
                            }
                        }

                        else -> throw e
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is ApiException -> {
                    // Errors to be processed after retrying
                    when (e.statusCode) {
                        ConnectionsStatusCodes.STATUS_RADIO_ERROR -> {
                            Log.e(TAG, "Radio error, closing connection", e)
                            close()
                        }

                        else -> throw e
                    }
                }

                else -> throw e
            }
        }
    }

    private val endpointDiscoveryCallbackDelegate = object : EndpointDiscoveryCallback() {

        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            _connectionScope?.launch(Dispatchers.IO) {
                Log.d(
                    "EndpointDiscoveryCallbackDelegate",
                    "onEndpointFound(endpointId=$endpointId, info=$info)"
                )
                this@NearbyConnectionManager.onEndpointFound(endpointId, info)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            _connectionScope?.launch(Dispatchers.IO) {
                Log.d("EndpointDiscoveryCallbackDelegate", "onEndpointLost(endpointId=$endpointId)")
                this@NearbyConnectionManager.onEndpointLost(endpointId)
            }
        }
    }

    private val connectionLifecycleCallbackDelegate = object : ConnectionLifecycleCallback() {

        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            _connectionScope?.launch(Dispatchers.IO) {
                Log.d(
                    "ConnectionLifecycleCallbackDelegate",
                    "onConnectionInitiated(endpointId=$endpointId, connectionInfo=$connectionInfo)"
                )
                this@NearbyConnectionManager.onConnectionInitiated(endpointId, connectionInfo)
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            _connectionScope?.launch(Dispatchers.IO) {
                Log.d(
                    "ConnectionLifecycleCallbackDelegate",
                    "onConnectionResult(endpointId=$endpointId, result=$result)"
                )
                this@NearbyConnectionManager.onConnectionResult(endpointId, result)
            }
        }

        override fun onDisconnected(endpointId: String) {
            _connectionScope?.launch(Dispatchers.IO) {
                Log.d(
                    "ConnectionLifecycleCallbackDelegate",
                    "onDisconnected(endpointId=$endpointId)"
                )
                this@NearbyConnectionManager.onDisconnected(endpointId)
            }
        }
    }

    private val payloadCallbackDelegate = object : PayloadCallback() {

        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            _connectionScope?.launch(Dispatchers.IO) {
                Log.d(
                    "PayloadCallbackDelegate",
                    "onPayloadReceived(endpointId=$endpointId, payload=$payload)"
                )
                this@NearbyConnectionManager.onPayloadReceived(endpointId, payload)
            }
        }

        override fun onPayloadTransferUpdate(
            endpointId: String,
            update: PayloadTransferUpdate,
        ) {
            _connectionScope?.launch(Dispatchers.IO) {
                Log.d(
                    "PayloadCallbackDelegate",
                    "onPayloadTransferUpdate(endpointId=$endpointId, update=$update)"
                )
                this@NearbyConnectionManager.onPayloadTransferUpdate(endpointId, update)
            }
        }
    }

    companion object {
        private val STRATEGY = Strategy.P2P_CLUSTER
        private const val PICTOCHAT_SERVICE_ID = "fr.outadoc.pictochat"
        private const val TAG = "NearbyConnectionManager"
    }
}