package fr.outadoc.pictochat.data

import android.net.wifi.aware.PeerHandle
import android.net.wifi.aware.PublishDiscoverySession
import android.net.wifi.aware.SubscribeDiscoverySession
import android.net.wifi.aware.WifiAwareSession

interface NearbyLifecycleCallbacks {
    suspend fun onAttached(session: WifiAwareSession)
    suspend fun onAttachFailed()
    suspend fun onAwareSessionTerminated()
    suspend fun onPublishStarted(session: PublishDiscoverySession)
    suspend fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray)
    suspend fun onMessageSendFailed(messageId: Int)
    suspend fun onMessageSendSucceeded(messageId: Int)
    suspend fun onSubscribeStarted(session: SubscribeDiscoverySession)
    suspend fun onServiceDiscovered(
        peerHandle: PeerHandle,
        serviceSpecificInfo: ByteArray,
        matchFilter: List<ByteArray>
    )
    suspend fun onServiceLost(peerHandle: PeerHandle, reason: Int)
}
