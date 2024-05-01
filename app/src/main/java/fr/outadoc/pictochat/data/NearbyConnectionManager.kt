package fr.outadoc.pictochat.data

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
import fr.outadoc.pictochat.DeviceNameProvider
import fr.outadoc.pictochat.domain.ConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NearbyConnectionManager(
    applicationContext: Context,
    private val deviceNameProvider: DeviceNameProvider,
) : ConnectionManager {

    private var connectionsClient: ConnectionsClient =
        Nearby.getConnectionsClient(applicationContext)

    private var _state: MutableStateFlow<ConnectionManager.State> =
        MutableStateFlow(ConnectionManager.State.Idle)
    override val state = _state.asStateFlow()

    private var discoveryJob: Job? = null
    private var advertisingJob: Job? = null

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(
                "NearbyConnectionManager",
                "onConnectionInitiated: $endpointId, ${connectionInfo.endpointName}"
            )
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
            Log.d("NearbyConnectionManager", "onEndpointFound: $endpointId, ${info.endpointName}")
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d("NearbyConnectionManager", "onEndpointLost: $endpointId")
        }
    }

    override suspend fun startDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = coroutineScope {
            launch(Dispatchers.IO) {
                Log.d("NearbyConnectionManager", "startDiscovery")
                connectionsClient
                    .startDiscovery(
                        BASE_SERVICE_ID,
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
                Log.d("NearbyConnectionManager", "startAdvertising")
                connectionsClient
                    .startAdvertising(
                        deviceNameProvider.getDeviceName(),
                        BASE_SERVICE_ID,
                        connectionLifecycleCallback,
                        AdvertisingOptions.Builder()
                            .setStrategy(STRATEGY)
                            .build()
                    )
                    .await()
            }
        }
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
        private const val BASE_SERVICE_ID = "fr.outadoc.pictochat"
    }
}