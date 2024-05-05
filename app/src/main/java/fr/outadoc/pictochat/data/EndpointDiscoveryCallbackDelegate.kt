package fr.outadoc.pictochat.data

import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EndpointDiscoveryCallbackDelegate(
    private val scope: CoroutineScope,
    private val delegate: NearbyLifecycleCallbacks,
) : EndpointDiscoveryCallback() {

    override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
        scope.launch(Dispatchers.IO) {
            delegate.onEndpointFound(endpointId, info)
        }
    }

    override fun onEndpointLost(endpointId: String) {
        scope.launch(Dispatchers.IO) {
            delegate.onEndpointLost(endpointId)
        }
    }
}