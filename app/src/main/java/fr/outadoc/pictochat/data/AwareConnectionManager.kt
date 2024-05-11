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
import android.util.Log
import androidx.core.content.getSystemService
import fr.outadoc.pictochat.domain.ConnectionManager
import fr.outadoc.pictochat.preferences.DeviceId
import fr.outadoc.pictochat.preferences.DeviceIdProvider
import fr.outadoc.pictochat.protocol.ChatPayload
import fr.outadoc.pictochat.protocol.EndpointInfoPayload
import fr.outadoc.pictochat.protocol.PayloadFragment
import fr.outadoc.pictochat.randomInt
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


@OptIn(ExperimentalSerializationApi::class, ExperimentalStdlibApi::class)
class AwareConnectionManager(
    applicationContext: Context,
    private val deviceIdProvider: DeviceIdProvider,
    private val clock: Clock,
) : ConnectionManager, NearbyLifecycleCallbacks {

    private data class RemoteDevice(
        val peerHandle: PeerHandle,
        val deviceId: DeviceId,
    )

    private data class InternalState(
        val isOnline: Boolean = false,
        val connectedPeers: PersistentSet<RemoteDevice> = persistentSetOf(),
        val partialFragments: PersistentMap<Int, PersistentList<PayloadFragment>> = persistentMapOf(),
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
                .build(),
            discoverySessionCallback,
            null
        )
    }

    override suspend fun onAttachFailed() {
    }

    override suspend fun onPublishStarted(session: PublishDiscoverySession) {
        _pubDiscoverySession?.close()
        _pubDiscoverySession = session
    }

    override suspend fun onSubscribeStarted(session: SubscribeDiscoverySession) {
        _subDiscoverySession?.close()
        _subDiscoverySession = session
    }

    @SuppressLint("MissingPermission")
    override suspend fun onServiceDiscovered(
        peerHandle: PeerHandle,
        serviceSpecificInfo: ByteArray,
        matchFilter: List<ByteArray>,
    ) {
        _stateLock.withLock {
            val decodedServiceSpecificInfo =
                ProtoBuf.decodeFromByteArray<EndpointInfoPayload>(serviceSpecificInfo)

            val sender = RemoteDevice(
                peerHandle = peerHandle,
                deviceId = DeviceId(decodedServiceSpecificInfo.deviceId)
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

            val fragments = encodePayloadToFragments(helloPayload)

            fragments.forEach { fragment ->
                _pubDiscoverySession?.sendMessage(
                    peerHandle,
                    randomInt(),
                    ProtoBuf.encodeToByteArray(fragment)
                )
            }
        }
    }

    override suspend fun onServiceLost(peerHandle: PeerHandle, reason: Int) {
        _stateLock.withLock {
            _state.update { state ->
                state.copy(
                    connectedPeers = state.connectedPeers.removeAll { it.peerHandle == peerHandle }
                )
            }
        }
    }

    override suspend fun onMessageSendFailed(messageId: Int) {

    }

    override suspend fun onMessageSendSucceeded(messageId: Int) {
    }

    override suspend fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
        _stateLock.withLock {
            val fragment = ProtoBuf.decodeFromByteArray<PayloadFragment>(message)

            Log.d(TAG, "onMessageReceived: $peerHandle, payload: $fragment")

            if (fragment.totalCount < 2) {
                // Single-fragment payload, no need to keep and manipulate a state
                tryDecodePayloadFragments(listOf(fragment))?.let { payload ->
                    _payloadFlow.emit(payload)
                }
            }

            val partialFragments: PersistentList<PayloadFragment> =
                _state.value.partialFragments.getOrDefault(fragment.id, persistentListOf())

            val updatedFragments: PersistentList<PayloadFragment> =
                partialFragments.add(fragment)

            val decoded = tryDecodePayloadFragments(updatedFragments)

            _state.update { state ->
                if (decoded != null) {
                    // We have all fragments, emit the payload and forget the partial fragments
                    _payloadFlow.emit(decoded)

                    state.copy(
                        partialFragments = state.partialFragments.remove(fragment.id)
                    )
                } else {
                    // We still need more fragments, keep them in the state for now
                    state.copy(
                        partialFragments = state.partialFragments.put(fragment.id, updatedFragments)
                    )
                }
            }
        }
    }

    private fun tryDecodePayloadFragments(fragments: List<PayloadFragment>): ChatPayload? {
        if (fragments.isEmpty()) {
            error("No fragments to decode")
        }

        val first = fragments.first()
        val totalFragments = first.totalCount
        val totalExpectedBytes = first.totalBytes

        // We assume that the list all concern the same payload without checking their IDs explicitly

        val orders = fragments
            .map { it.order }
            .sorted()
            .distinct()

        if (orders.size > totalFragments) {
            Log.w(TAG, "Expected only $totalFragments fragments, got: $orders")
            return null
        }

        if (orders.size < totalFragments) {
            Log.w(TAG, "Still missing fragments, expected $totalFragments, got: $orders")
            return null
        }

        val buffer = ByteArray(totalExpectedBytes)
        var lastOffset = 0

        fragments
            .sortedBy { it.order }
            .distinctBy { it.order }
            .forEach { fragment ->
                fragment.data.copyInto(
                    destination = buffer,
                    destinationOffset = lastOffset
                )

                lastOffset = fragment.data.size
            }

        return ProtoBuf.decodeFromByteArray(decompress(buffer))
    }

    private fun encodePayloadToFragments(payload: ChatPayload): List<PayloadFragment> {
        val payloadBytes = compress(ProtoBuf.encodeToByteArray(payload))
        val totalBytes = payloadBytes.size

        val maxFragmentSize: Int = 127

        val totalFragments: Int = (totalBytes / maxFragmentSize) + 1

        val fragments = (0 until totalFragments).map { fragmentIndex ->
            val start = fragmentIndex * maxFragmentSize
            val end = start + maxFragmentSize

            val fragmentData = payloadBytes.sliceArray(start until (end.coerceAtMost(totalBytes)))

            PayloadFragment(
                id = payload.id,
                order = fragmentIndex,
                totalCount = totalFragments,
                totalBytes = totalBytes,
                data = fragmentData
            )
        }

        return fragments
    }

    override suspend fun onAwareSessionTerminated() {
        _state.updateAndGet { state ->
            state.copy(
                connectedPeers = persistentSetOf()
            )
        }

        doConnect()
    }

    override suspend fun connect() = withContext(Dispatchers.IO) {
        Log.d(TAG, "connect: deviceId=${deviceIdProvider.deviceId}")

        _connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        _state.update { state ->
            state.copy(
                isOnline = true,
                connectedPeers = persistentSetOf()
            )
        }

        doConnect()

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
        _stateLock.withLock {
            _state.value.connectedPeers.forEach { peer ->
                sendPayloadTo(peer.peerHandle, payload)
            }
        }
    }

    private suspend fun sendPayloadTo(destination: PeerHandle, payload: ChatPayload) {
        val fragments = encodePayloadToFragments(payload)
        val firstFragment = fragments.first()

        Log.d(
            TAG,
            "Sending payload (${firstFragment.totalBytes} bytes split into ${firstFragment.totalCount} fragments) to $destination: $payload"
        )

        fragments.forEach { fragment ->
            val protoBytes = ProtoBuf.encodeToByteArray(fragment)
            _pubDiscoverySession?.sendMessage(
                destination,
                randomInt(),
                protoBytes
            )
            delay(100)
        }
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
        private const val TAG = "NearbyConnectionManager"

        /**
         * The overhead of a single fragment in bytes.
         */
        private val fragmentLengthOverhead: Int =
            ProtoBuf.encodeToByteArray(
                PayloadFragment(
                    id = 0,
                    order = 0,
                    totalCount = 0,
                    totalBytes = 0,
                    data = ByteArray(0)
                )
            ).size
    }
}