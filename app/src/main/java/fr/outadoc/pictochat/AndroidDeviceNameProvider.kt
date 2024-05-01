package fr.outadoc.pictochat

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.getSystemService

/**
 * Returns the name of the device, using the Bluetooth APIs, and falling back on the device's model.
 */
class AndroidDeviceNameProvider(
    private val applicationContext: Context,
) : DeviceNameProvider {

    private var cachedName: String? = null

    override fun getDeviceName(): String {
        return cachedName ?: getAndroidDeviceName().also { name -> cachedName = name }
    }

    private fun getAndroidDeviceName(): String {
        val permissionName = if (Build.VERSION.SDK_INT >= 31) {
            Manifest.permission.BLUETOOTH_CONNECT
        } else {
            Manifest.permission.BLUETOOTH
        }

        val bluetoothPermission: Int = applicationContext.checkSelfPermission(permissionName)

        val bluetoothName: String? =
            if (bluetoothPermission == PackageManager.PERMISSION_GRANTED) {
                val bluetoothManager = applicationContext.getSystemService<BluetoothManager>()
                bluetoothManager?.adapter?.name
            } else {
                null
            }

        return bluetoothName ?: "${Build.MANUFACTURER} ${Build.MODEL}"
    }
}