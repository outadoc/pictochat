package fr.outadoc.pictochat.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.aware.AttachCallback
import android.net.wifi.aware.DiscoverySession
import android.net.wifi.aware.DiscoverySessionCallback
import android.net.wifi.aware.PeerHandle
import android.net.wifi.aware.PublishConfig
import android.net.wifi.aware.PublishDiscoverySession
import android.net.wifi.aware.SubscribeConfig
import android.net.wifi.aware.SubscribeDiscoverySession
import android.net.wifi.aware.WifiAwareManager
import android.net.wifi.aware.WifiAwareSession
import android.util.Log
import androidx.core.content.getSystemService
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class, ExperimentalStdlibApi::class)
class AwareConnectionManager(
    applicationContext: Context,
    private val deviceIdProvider: DeviceIdProvider,
) : ConnectionManager, NearbyLifecycleCallbacks {

    private var _state: MutableStateFlow<ConnectionManager.State> =
        MutableStateFlow(ConnectionManager.State())
    override val state = _state.asStateFlow()

    private val stateLock = Mutex()

    private val _payloadFlow = MutableSharedFlow<ReceivedPayload>(extraBufferCapacity = 32)
    override val payloadFlow = _payloadFlow.asSharedFlow()

    private val connectionsClient = applicationContext.getSystemService<WifiAwareManager>()
        ?: error("WifiAwareManager not available")

    private var awareSession: WifiAwareSession? = null
    private var discoverySession: DiscoverySession? = null

    private var _connectionScope: CoroutineScope? = null

    @SuppressLint("MissingPermission")
    override suspend fun onAttached(session: WifiAwareSession) {
        val endpointInfo = EndpointInfoPayload(
            deviceId = deviceIdProvider.deviceId.value
        )

        val payload = ProtoBuf.encodeToByteArray(endpointInfo)

        session.publish(
            PublishConfig.Builder()
                .setServiceName(PICTOCHAT_SERVICE_ID)
                .setServiceSpecificInfo(payload)
                .build(),
            discoverySessionCallback,
            null
        )

        session.subscribe(
            SubscribeConfig.Builder()
                .setServiceName(PICTOCHAT_SERVICE_ID)
                .setServiceSpecificInfo(payload)
                .build(),
            discoverySessionCallback,
            null
        )
    }

    override suspend fun onAttachFailed() {
    }

    override suspend fun onPublishStarted(session: PublishDiscoverySession) {
    }

    override suspend fun onSubscribeStarted(session: SubscribeDiscoverySession) {
        discoverySession = session
    }

    @SuppressLint("MissingPermission")
    override suspend fun onServiceDiscovered(
        peerHandle: PeerHandle,
        serviceSpecificInfo: ByteArray,
        matchFilter: List<ByteArray>,
    ) {
        stateLock.withLock {
            val endpointInfo = try {
                ProtoBuf.decodeFromByteArray<EndpointInfoPayload>(serviceSpecificInfo)
            } catch (e: SerializationException) {
                Log.e(TAG, "Failed to decode endpoint info", e)
                return
            }

            val remoteDevice = RemoteDevice(
                deviceId = DeviceId(endpointInfo.deviceId),
                endpointId = peerHandle
            )

            _state.update { state ->
                state.copy(
                    connectedEndpoints = state.connectedEndpoints.add(remoteDevice)
                )
            }
        }
    }

    override suspend fun onServiceLost(peerHandle: PeerHandle, reason: Int) {
        stateLock.withLock {
            _state.update { state ->
                state.copy(
                    connectedEndpoints = state.connectedEndpoints.removeAll { it.endpointId == peerHandle }
                )
            }
        }
    }

    override suspend fun onMessageSendFailed(messageId: Int) {
    }

    override suspend fun onMessageSendSucceeded(messageId: Int) {
    }

    override suspend fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
        stateLock.withLock {
            val proto = ProtoBuf.decodeFromByteArray<ChatPayload>(message)
            Log.d(TAG, "onMessageReceived: $peerHandle, payload: $proto")

            val sender = _state.value.connectedEndpoints.first { it.endpointId == peerHandle }

            _payloadFlow.tryEmit(
                ReceivedPayload(
                    sender = _state.value.connectedEndpoints.first { it.endpointId == peerHandle },
                    data = proto
                )
            )

            _state.value.connectedEndpoints.forEach { device ->
                if (device.endpointId != peerHandle && device.deviceId != sender.deviceId) {
                    sendPayload(
                        endpointId = device,
                        payload = proto
                    )
                }
            }
        }
    }

    override suspend fun onAwareSessionTerminated() {
    }

    override suspend fun connect() {
        coroutineScope {
            _connectionScope?.cancel()
            _connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            launch(Dispatchers.IO) {
                Log.d(TAG, "connect: deviceId=${deviceIdProvider.deviceId}")

                _state.update { state ->
                    state.copy(
                        isOnline = true,
                        connectedEndpoints = persistentSetOf()
                    )
                }

                // TODO use another handler
                connectionsClient.attach(attachCallback, null)

                try {
                    awaitCancellation()
                } catch (e: Exception) {
                    Log.d(TAG, "Connection cancelled")
                    close()
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun sendPayload(endpointId: RemoteDevice, payload: ChatPayload) {
        val protoBytes = ProtoBuf.encodeToByteArray(payload)
        Log.d(TAG, "Sending payload to $endpointId: $payload")

        /*discoverySession?.sendMessage(
            endpointId.endpointId,
            payload.id.hashCode(),
            protoBytes
        )*/
    }

    override fun close() {
        Log.d(TAG, "Closing all connections")

        awareSession?.close()
        _connectionScope?.cancel()

        _state.updateAndGet { state ->
            state.copy(
                isOnline = false,
                connectedEndpoints = persistentSetOf()
            )
        }
    }

    private val discoverySessionCallback = object : DiscoverySessionCallback() {

        override fun onPublishStarted(session: PublishDiscoverySession) {
            _connectionScope?.launch(Dispatchers.IO) {
                Log.d(TAG, "onPublishStarted(session=$session)")
                this@AwareConnectionManager.onPublishStarted(session)
            }
        }

        override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
            _connectionScope?.launch(Dispatchers.IO) {
                Log.d(
                    TAG,
                    "onMessageReceived(peerHandle=$peerHandle, message=${message?.toHexString()})"
                )
                this@AwareConnectionManager.onMessageReceived(peerHandle, message)
            }
        }

        override fun onMessageSendFailed(messageId: Int) {
            _connectionScope?.launch(Dispatchers.IO) {
                Log.d(TAG, "onMessageSendFailed(messageId=$messageId)")
                this@AwareConnectionManager.onMessageSendFailed(messageId)
            }
        }

        override fun onMessageSendSucceeded(messageId: Int) {
            _connectionScope?.launch(Dispatchers.IO) {
                Log.d(TAG, "onMessageSendSucceeded(messageId=$messageId)")
                this@AwareConnectionManager.onMessageSendSucceeded(messageId)
            }
        }

        override fun onSubscribeStarted(session: SubscribeDiscoverySession) {
            _connectionScope?.launch(Dispatchers.IO) {
                Log.d(TAG, "onSubscribeStarted(session=$session)")
                this@AwareConnectionManager.onSubscribeStarted(session)
            }
        }

        override fun onServiceDiscovered(
            peerHandle: PeerHandle,
            serviceSpecificInfo: ByteArray,
            matchFilter: List<ByteArray>,
        ) {
            _connectionScope?.launch(Dispatchers.IO) {
                Log.d(
                    TAG,
                    "onServiceDiscovered(peerHandle=$peerHandle, serviceSpecificInfo=${serviceSpecificInfo.toHexString()}, matchFilter=${matchFilter})"
                )
                this@AwareConnectionManager.onServiceDiscovered(
                    peerHandle,
                    serviceSpecificInfo,
                    matchFilter
                )
            }
        }

        override fun onServiceLost(peerHandle: PeerHandle, reason: Int) {
            _connectionScope?.launch(Dispatchers.IO) {
                Log.d(TAG, "onServiceLost(peerHandle=$peerHandle, reason=$reason)")
                this@AwareConnectionManager.onServiceLost(peerHandle, reason)
            }
        }
    }

    private val attachCallback = object : AttachCallback() {

        override fun onAttached(session: WifiAwareSession) {
            _connectionScope?.launch(Dispatchers.IO) {
                Log.d(TAG, "onAttached(session=$session)")
                this@AwareConnectionManager.onAttached(session)
            }
        }

        override fun onAttachFailed() {
            _connectionScope?.launch(Dispatchers.IO) {
                Log.d(TAG, "onAttachFailed")
                this@AwareConnectionManager.onAttachFailed()
            }
        }

        override fun onAwareSessionTerminated() {
            _connectionScope?.launch(Dispatchers.IO) {
                Log.d(TAG, "onAwareSessionTerminated")
                this@AwareConnectionManager.onAwareSessionTerminated()
            }
        }
    }

    companion object {
        private const val PICTOCHAT_SERVICE_ID = "fr.outadoc.pictochat"
        private const val TAG = "NearbyConnectionManager"
    }
}