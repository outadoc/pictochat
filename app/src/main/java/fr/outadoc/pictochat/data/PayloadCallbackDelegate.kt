package fr.outadoc.pictochat.data

import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PayloadCallbackDelegate(
    private val scope: CoroutineScope,
    private val delegate: NearbyLifecycleCallbacks,
) : PayloadCallback() {

    override fun onPayloadReceived(endpointId: String, payload: Payload) {
        scope.launch(Dispatchers.IO) {
            delegate.onPayloadReceived(endpointId, payload)
        }
    }

    override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
        scope.launch(Dispatchers.IO) {
            delegate.onPayloadTransferUpdate(endpointId, update)
        }
    }
}
