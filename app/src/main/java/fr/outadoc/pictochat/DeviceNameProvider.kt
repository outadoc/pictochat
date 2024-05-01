package fr.outadoc.pictochat

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.getSystemService

interface DeviceNameProvider {
    fun getDeviceName(): String
}

/**
 * Returns the name of the device, using the Bluetooth APIs, and falling back on the device's model.
 */
class AndroidDeviceNameProvider(
    private val applicationContext: Context,
): DeviceNameProvider {

    override fun getDeviceName(): String {
        val bluetoothPermission =
            applicationContext.checkSelfPermission(Manifest.permission.BLUETOOTH)
        val bluetoothName =
            if (bluetoothPermission == PackageManager.PERMISSION_GRANTED) {
                val bluetoothManager = applicationContext.getSystemService<BluetoothManager>()
                bluetoothManager?.adapter?.name
            } else {
                null
            }

        return bluetoothName ?: "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
    }
}