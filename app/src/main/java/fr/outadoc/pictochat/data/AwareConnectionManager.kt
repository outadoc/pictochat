package fr.outadoc.pictochat.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.aware.AttachCallback
import android.net.wifi.aware.DiscoverySessionCallback
import android.net.wifi.aware.PeerHandle
import android.net.wifi.aware.PublishConfig
import android.net.wifi.aware.PublishDiscoverySession
import android.net.wifi.aware.SubscribeConfig
import android.net.wifi.aware.SubscribeDiscoverySession
import android.net.wifi.aware.WifiAwareManager
import android.net.wifi.aware.WifiAwareSession
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import fr.outadoc.pictochat.domain.ConnectionManager
import fr.outadoc.pictochat.preferences.DeviceId
import fr.outadoc.pictochat.preferences.DeviceIdProvider
import fr.outadoc.pictochat.protocol.ChatPayload
import fr.outadoc.pictochat.protocol.EndpointInfoPayload
import fr.outadoc.pictochat.randomInt
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalSerializationApi::class, ExperimentalStdlibApi::class)
class AwareConnectionManager(
    applicationContext: Context,
    private val deviceIdProvider: DeviceIdProvider,
    private val clock: Clock,
) : ConnectionManager {

    private data class RemoteDevice(
        val peerHandle: PeerHandle,
        val deviceId: DeviceId,
    )

    private data class InternalState(
        val isOnline: Boolean = false,
        val connectedPeers: PersistentSet<RemoteDevice> = persistentSetOf(),
    )

    private val _stateLock = Mutex()
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

    private val _awareClient = applicationContext.getSystemService<WifiAwareManager>()
        ?: error("WifiAwareManager not available")

    private var _awareSession: WifiAwareSession? = null
    private var _pubDiscoverySession: PublishDiscoverySession? = null
    private var _subDiscoverySession: SubscribeDiscoverySession? = null

    private var _connectionScope: CoroutineScope? = null

    @SuppressLint("MissingPermission")
    private suspend fun onAttached(session: WifiAwareSession) {
        val endpointInfo = EndpointInfoPayload(
            deviceId = deviceIdProvider.deviceId
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
                .build(),
            discoverySessionCallback,
            null
        )
    }

    private suspend fun onAttachFailed() {
    }

    private suspend fun onPublishStarted(session: PublishDiscoverySession) {
        _pubDiscoverySession?.close()
        _pubDiscoverySession = session
    }

    private suspend fun onSubscribeStarted(session: SubscribeDiscoverySession) {
        _subDiscoverySession?.close()
        _subDiscoverySession = session
    }

    @SuppressLint("MissingPermission")
    private suspend fun onServiceDiscovered(
        peerHandle: PeerHandle,
        serviceSpecificInfo: ByteArray,
        matchFilter: List<ByteArray>,
    ) {
        _stateLock.withLock {
            val decodedServiceSpecificInfo =
                ProtoBuf.decodeFromByteArray<EndpointInfoPayload>(serviceSpecificInfo)

            val sender = RemoteDevice(
                peerHandle = peerHandle,
                deviceId = decodedServiceSpecificInfo.deviceId
            )

            _state.update { state ->
                state.copy(
                    connectedPeers = state.connectedPeers
                        .removeAll { it.deviceId == sender.deviceId }
                        .add(sender)
                )
            }

            val helloPayload = ChatPayload.Hello(
                id = randomInt(),
                source = deviceIdProvider.deviceId,
                sentAt = clock.now()
            )

            _subDiscoverySession?.sendMessage(
                peerHandle,
                helloPayload.id.hashCode(),
                compress(ProtoBuf.encodeToByteArray<ChatPayload>(helloPayload))
            )
        }
    }

    private suspend fun onServiceLost(peerHandle: PeerHandle, reason: Int) {
        _stateLock.withLock {
            _state.update { state ->
                state.copy(
                    connectedPeers = state.connectedPeers.removeAll { it.peerHandle == peerHandle }
                )
            }
        }
    }

    private suspend fun onMessageSendFailed(messageId: Int) {

    }

    private suspend fun onMessageSendSucceeded(messageId: Int) {
    }

    private suspend fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
        _stateLock.withLock {
            val payload = ProtoBuf.decodeFromByteArray<ChatPayload>(decompress(message))

            Log.d(TAG, "onMessageReceived: $peerHandle, payload: $payload")

            _payloadFlow.tryEmit(payload)
        }
    }

    private suspend fun onAwareSessionTerminated() {
        _state.updateAndGet { state ->
            state.copy(
                connectedPeers = persistentSetOf()
            )
        }

        doConnect()
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
                        connectedPeers = persistentSetOf()
                    )
                }

                doConnect()
            }
        }

        try {
            awaitCancellation()
        } finally {
            Log.d(TAG, "Connection cancelled")
            close()
        }
    }

    private fun doConnect() {
        _awareClient.attach(attachCallback, null)
    }

    override suspend fun broadcast(payload: ChatPayload) {
        // Echo message locally
        _payloadFlow.tryEmit(payload)

        _stateLock.withLock {
            _state.value.connectedPeers.forEach { peer ->
                sendPayloadTo(peer.peerHandle, payload)
            }
        }
    }

    override suspend fun send(recipient: DeviceId, payload: ChatPayload) {
        _stateLock.withLock {
            val recipientPeer = _state.value.connectedPeers
                .firstOrNull { it.deviceId == recipient }

            if (recipientPeer == null) {
                Log.e(TAG, "Recipient $recipient not found in connected peers")
                return
            }

            sendPayloadTo(
                destination = recipientPeer.peerHandle,
                payload = payload
            )
        }
    }

    private fun sendPayloadTo(destination: PeerHandle, payload: ChatPayload) {
        val protoBytes = compress(ProtoBuf.encodeToByteArray(payload))

        Log.d(TAG, "Sending payload (${protoBytes.size} bytes) to $destination: $payload")

        _subDiscoverySession?.sendMessage(
            destination,
            payload.id.hashCode(),
            protoBytes
        )
    }

    override fun close() {
        Log.d(TAG, "Closing all connections")

        _pubDiscoverySession?.close()
        _pubDiscoverySession = null

        _subDiscoverySession?.close()
        _subDiscoverySession = null

        _awareSession?.close()
        _awareSession = null

        _connectionScope?.cancel()
        _connectionScope = null

        _state.updateAndGet { state ->
            state.copy(
                isOnline = false,
                connectedPeers = persistentSetOf()
            )
        }
    }

    /**
     *Compresses a byte array using GZIP.
     *
     * @param data The byte array to compress.
     * @return The compressed byte array.
     */
    private fun compress(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }

    /**
     * Decompresses a byte array using GZIP.
     *
     * @param compressed The compressed byte array.
     * @return The uncompressed byte array.
     */
    private fun decompress(compressed: ByteArray): ByteArray {
        val bis = ByteArrayInputStream(compressed)
        GZIPInputStream(bis).use {
            return it.readBytes()
        }
    }


    private val discoverySessionCallback = object : DiscoverySessionCallback() {

        override fun onPublishStarted(session: PublishDiscoverySession) {
            _connectionScope?.launch {
                Log.d(TAG, "onPublishStarted(session=$session)")
                this@AwareConnectionManager.onPublishStarted(session)
            }
        }

        override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
            _connectionScope?.launch {
                Log.d(
                    TAG,
                    "onMessageReceived(peerHandle=$peerHandle, message=${message.toHexString()})"
                )
                this@AwareConnectionManager.onMessageReceived(peerHandle, message)
            }
        }

        override fun onMessageSendFailed(messageId: Int) {
            _connectionScope?.launch {
                Log.d(TAG, "onMessageSendFailed(messageId=$messageId)")
                this@AwareConnectionManager.onMessageSendFailed(messageId)
            }
        }

        override fun onMessageSendSucceeded(messageId: Int) {
            _connectionScope?.launch {
                Log.d(TAG, "onMessageSendSucceeded(messageId=$messageId)")
                this@AwareConnectionManager.onMessageSendSucceeded(messageId)
            }
        }

        override fun onSubscribeStarted(session: SubscribeDiscoverySession) {
            _connectionScope?.launch {
                Log.d(TAG, "onSubscribeStarted(session=$session)")
                this@AwareConnectionManager.onSubscribeStarted(session)
            }
        }

        override fun onServiceDiscovered(
            peerHandle: PeerHandle,
            serviceSpecificInfo: ByteArray,
            matchFilter: List<ByteArray>,
        ) {
            _connectionScope?.launch {
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
            _connectionScope?.launch {
                Log.d(TAG, "onServiceLost(peerHandle=$peerHandle, reason=$reason)")
                this@AwareConnectionManager.onServiceLost(peerHandle, reason)
            }
        }
    }

    private val attachCallback = object : AttachCallback() {

        override fun onAttached(session: WifiAwareSession) {
            _connectionScope?.launch {
                Log.d(TAG, "onAttached(session=$session)")
                this@AwareConnectionManager.onAttached(session)
            }
        }

        override fun onAttachFailed() {
            _connectionScope?.launch {
                Log.d(TAG, "onAttachFailed")
                this@AwareConnectionManager.onAttachFailed()
            }
        }

        override fun onAwareSessionTerminated() {
            _connectionScope?.launch {
                Log.d(TAG, "onAwareSessionTerminated")
                this@AwareConnectionManager.onAwareSessionTerminated()
            }
        }
    }

    companion object {
        private const val PICTOCHAT_SERVICE_ID = "fr.outadoc.pictochat"
        private const val TAG = "AwareConnectionManager"
    }
}