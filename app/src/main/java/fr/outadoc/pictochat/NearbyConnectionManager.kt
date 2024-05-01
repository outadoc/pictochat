package fr.outadoc.pictochat

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.Closeable

interface NearbyConnectionManager : Closeable {
    suspend fun start()
}

sealed interface NearbyConnectionState {
    data object Idle : NearbyConnectionState
}

class NearbyConnectionManagerImpl(
    applicationContext: Context,
    private val deviceNameProvider: DeviceNameProvider,
) : NearbyConnectionManager {

    private var connectionsClient: ConnectionsClient =
        Nearby.getConnectionsClient(applicationContext)

    private var _state: MutableStateFlow<NearbyConnectionState> =
        MutableStateFlow(NearbyConnectionState.Idle)
    val state = _state.asStateFlow()

    private var job: Job? = null

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d("NearbyConnectionManager", "onConnectionInitiated: $endpointId")
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            Log.d("NearbyConnectionManager", "onConnectionResult: $endpointId")
        }

        override fun onDisconnected(endpointId: String) {
            Log.d("NearbyConnectionManager", "onDisconnected: $endpointId")
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d("NearbyConnectionManager", "onEndpointFound: $endpointId")
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d("NearbyConnectionManager", "onEndpointLost: $endpointId")
        }
    }

    override suspend fun start() {
        job?.cancel()
        job = coroutineScope {
            launch(Dispatchers.IO) {
                connectionsClient
                    .startAdvertising(
                        deviceNameProvider.getDeviceName(),
                        SERVICE_ID,
                        connectionLifecycleCallback,
                        AdvertisingOptions.Builder()
                            .setStrategy(STRATEGY)
                            .build()
                    )
                    .await()
            }

            launch {
                connectionsClient
                    .startDiscovery(
                        SERVICE_ID,
                        endpointDiscoveryCallback,
                        DiscoveryOptions.Builder()
                            .setStrategy(STRATEGY)
                            .build()
                    )
                    .await()
            }
        }
    }

    override fun close() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
    }

    companion object {
        private const val SERVICE_ID = "fr.outadoc.pictochat"
        private val STRATEGY = Strategy.P2P_CLUSTER
    }
}