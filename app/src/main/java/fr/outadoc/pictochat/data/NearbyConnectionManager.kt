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
import fr.outadoc.pictochat.preferences.DeviceId
import fr.outadoc.pictochat.preferences.DeviceIdProvider
import fr.outadoc.pictochat.protocol.ChatPayload
import fr.outadoc.pictochat.protocol.EndpointInfoPayload
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
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
) : ConnectionManager {

    private data class RemoteDevice(
        val endpointId: String,
        val deviceId: DeviceId,
    )

    private data class InternalState(
        val isOnline: Boolean = false,
        val connectedPeers: PersistentSet<RemoteDevice> = persistentSetOf(),
        val knownPeers: PersistentMap<String, DeviceId> = persistentMapOf(),
    )

    private var _state = MutableStateFlow(InternalState())

    override val state = _state
        .map { internalState ->
            ConnectionManager.State(
                isOnline = internalState.isOnline,
                connectedPeers = internalState.connectedPeers.map { it.deviceId }.toPersistentSet()
            )
        }

    private val _payloadFlow = MutableSharedFlow<ChatPayload>(extraBufferCapacity = 32)
    override val payloadFlow = _payloadFlow.asSharedFlow()

    private val connectionsClient: ConnectionsClient =
        Nearby.getConnectionsClient(applicationContext)

    private var _connectionScope: CoroutineScope? = null

    private suspend fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
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
            deviceId = decoded.deviceId
        )

        _state.update { state ->
            Log.d(TAG, "onConnectionInitiated: Current state: $state")

            state.copy(
                knownPeers = state.knownPeers.put(endpointId, device.deviceId)
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

    private suspend fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
        _state.update { state ->
            val deviceId = state.knownPeers[endpointId]

            if (deviceId == null) {
                Log.d(TAG, "Ignoring connection result for unknown endpoint $endpointId")
                return@update state
            }

            val device = RemoteDevice(endpointId, deviceId)

            Log.d(
                TAG,
                "Connection result for $endpointId: ${
                    ConnectionsStatusCodes.getStatusCodeString(result.status.statusCode)
                }"
            )

            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK,
                ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT,
                -> {
                    state.copy(
                        connectedPeers = state.connectedPeers.add(device)
                    )
                }

                else -> {
                    state.copy(
                        connectedPeers = state.connectedPeers.remove(device)
                    )
                }
            }
        }
    }

    private suspend fun onDisconnected(endpointId: String) {
        _state.update { state ->
            state.copy(
                connectedPeers = state.connectedPeers.removeAll { it.endpointId == endpointId },
            )
        }
    }

    private suspend fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
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
            deviceId = decoded.deviceId
        )

        Log.i(TAG, "onEndpointFound: $discoveredDevice, got payload $decoded")

        val payload = EndpointInfoPayload(deviceId = deviceIdProvider.deviceId)

        Log.i(TAG, "Requesting connection to $discoveredDevice: sending $payload")

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

    private suspend fun onEndpointLost(endpointId: String) {
    }

    private suspend fun onPayloadReceived(endpointId: String, payload: Payload) {
        val proto = ProtoBuf.decodeFromByteArray<ChatPayload>(payload.asBytes()!!)

        Log.d(TAG, "onPayloadReceived: $endpointId, payload: $proto")

        val senderDeviceId: DeviceId? = _state.value.knownPeers[endpointId]

        _payloadFlow.tryEmit(proto)

        _state.value.connectedPeers.forEach { device ->
            if (device.endpointId != endpointId &&
                device.deviceId != senderDeviceId &&
                device.deviceId != proto.source
            ) {
                sendPayloadTo(device.endpointId, proto)
            }
        }
    }

    private suspend fun onPayloadTransferUpdate(
        endpointId: String,
        update: PayloadTransferUpdate,
    ) {
    }

    override suspend fun connect() {
        coroutineScope {
            _connectionScope?.cancel()
            _connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            _connectionScope?.launch {
                Log.d(TAG, "connect: deviceId=${deviceIdProvider.deviceId}")

                _state.update { state ->
                    state.copy(
                        isOnline = true,
                        connectedPeers = persistentSetOf(),
                    )
                }

                launch {
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

                launch {
                    val endpointInfo = EndpointInfoPayload(
                        deviceId = deviceIdProvider.deviceId
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
                }
            }
        }

        try {
            awaitCancellation()
        } catch (e: Exception) {
            Log.d(TAG, "Connection cancelled")
            close()
        }
    }

    override suspend fun broadcast(payload: ChatPayload) {
        _state.value.connectedPeers.forEach { peer ->
            sendPayloadTo(peer.endpointId, payload)
        }
    }

    private suspend fun sendPayloadTo(endpointId: String, payload: ChatPayload) {
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
                connectedPeers = persistentSetOf(),
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
            _connectionScope?.launch {
                Log.d(TAG, "onEndpointFound(endpointId=$endpointId, info=$info)")
                this@NearbyConnectionManager.onEndpointFound(endpointId, info)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            _connectionScope?.launch {
                Log.d(TAG, "onEndpointLost(endpointId=$endpointId)")
                this@NearbyConnectionManager.onEndpointLost(endpointId)
            }
        }
    }

    private val connectionLifecycleCallbackDelegate = object : ConnectionLifecycleCallback() {

        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            _connectionScope?.launch {
                Log.d(
                    TAG,
                    "onConnectionInitiated(endpointId=$endpointId, connectionInfo=$connectionInfo)"
                )
                this@NearbyConnectionManager.onConnectionInitiated(endpointId, connectionInfo)
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            _connectionScope?.launch {
                Log.d(TAG, "onConnectionResult(endpointId=$endpointId, result=$result)")
                this@NearbyConnectionManager.onConnectionResult(endpointId, result)
            }
        }

        override fun onDisconnected(endpointId: String) {
            _connectionScope?.launch {
                Log.d(TAG, "onDisconnected(endpointId=$endpointId)")
                this@NearbyConnectionManager.onDisconnected(endpointId)
            }
        }
    }

    private val payloadCallbackDelegate = object : PayloadCallback() {

        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            _connectionScope?.launch {
                Log.d(TAG, "onPayloadReceived(endpointId=$endpointId, payload=$payload)")
                this@NearbyConnectionManager.onPayloadReceived(endpointId, payload)
            }
        }

        override fun onPayloadTransferUpdate(
            endpointId: String,
            update: PayloadTransferUpdate,
        ) {
            _connectionScope?.launch {
                Log.d(TAG, "onPayloadTransferUpdate(endpointId=$endpointId, update=$update)")
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
