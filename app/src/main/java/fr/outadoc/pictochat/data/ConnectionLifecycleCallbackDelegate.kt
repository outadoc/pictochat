package fr.outadoc.pictochat.data

import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConnectionLifecycleCallbackDelegate(
    private val scope: CoroutineScope,
    private val delegate: NearbyLifecycleCallbacks,
) : ConnectionLifecycleCallback() {

    override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
        scope.launch(Dispatchers.IO) {
            delegate.onConnectionInitiated(endpointId, connectionInfo)
        }
    }

    override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
        scope.launch(Dispatchers.IO) {
            delegate.onConnectionResult(endpointId, result)
        }
    }

    override fun onDisconnected(endpointId: String) {
        scope.launch(Dispatchers.IO) {
            delegate.onDisconnected(endpointId)
        }
    }
}