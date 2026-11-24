package com.proyectos.tvremote.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class BluetoothHidState {
    object NotSupported : BluetoothHidState()
    object BluetoothOff : BluetoothHidState()
    object Registering : BluetoothHidState()
    data class WaitingForConnection(val localName: String) : BluetoothHidState()
    data class Connected(val deviceName: String) : BluetoothHidState()
    data class Error(val message: String) : BluetoothHidState()
}

/**
 * Registers this phone as a Bluetooth HID peripheral (keyboard + consumer-control
 * combo, see [HidReportDescriptor]) so a TV can pair with it from its own
 * "add Bluetooth remote" menu, same as any physical BT remote/keyboard.
 *
 * Needs API 28+ (BluetoothHidDevice). Caller must check [isSupported] first and
 * hold BLUETOOTH_CONNECT (API 31+) before calling [register].
 */
class BluetoothHidController(private val context: Context) {

    var onStateChanged: ((BluetoothHidState) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(state)
        }

    var state: BluetoothHidState = BluetoothHidState.Registering
        private set(value) {
            field = value
            onStateChanged?.invoke(value)
        }

    private val adapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private val mainExecutor = ContextCompat.getMainExecutor(context)

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        @SuppressLint("MissingPermission")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDevice = proxy as BluetoothHidDevice
            val sdp = BluetoothHidDeviceAppSdpSettings(
                "TV Remote",
                "Control remoto por Bluetooth",
                "proyectos.tvremote",
                BluetoothHidDevice.SUBCLASS1_COMBO,
                HidReportDescriptor.DESCRIPTOR
            )
            try {
                hidDevice?.registerApp(sdp, null, null, mainExecutor, hidCallback)
            } catch (e: SecurityException) {
                state = BluetoothHidState.Error("Falta permiso de Bluetooth: ${e.message}")
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            hidDevice = null
            connectedDevice = null
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        @SuppressLint("MissingPermission")
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            state = if (registered) {
                BluetoothHidState.WaitingForConnection(adapter?.name ?: "este celular")
            } else {
                BluetoothHidState.Error("No se pudo registrar el perfil HID")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChanged(device: BluetoothDevice?, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    state = BluetoothHidState.Connected(device?.name ?: device?.address ?: "TV")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedDevice?.address == device?.address) connectedDevice = null
                    state = BluetoothHidState.WaitingForConnection(adapter?.name ?: "este celular")
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {}
        override fun onSetProtocol(device: BluetoothDevice?, protocol: Byte) {}
        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {}
        override fun onVirtualCableUnplug(device: BluetoothDevice?) {
            connectedDevice = null
            state = BluetoothHidState.WaitingForConnection(adapter?.name ?: "este celular")
        }
    }

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && adapter != null

    @SuppressLint("MissingPermission")
    fun register() {
        val bt = adapter
        if (!isSupported() || bt == null) {
            state = BluetoothHidState.NotSupported
            return
        }
        if (!bt.isEnabled) {
            state = BluetoothHidState.BluetoothOff
            return
        }
        state = BluetoothHidState.Registering
        try {
            bt.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)
        } catch (e: SecurityException) {
            state = BluetoothHidState.Error("Falta permiso de Bluetooth: ${e.message}")
        }
    }

    fun enableBluetoothIntent(): Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

    fun discoverableIntent(durationSeconds: Int = 300): Intent =
        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, durationSeconds)

    @SuppressLint("MissingPermission")
    fun close() {
        try {
            hidDevice?.unregisterApp()
        } catch (_: SecurityException) {
        }
        adapter?.let {
            try {
                it.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
            } catch (_: Exception) {
            }
        }
        hidDevice = null
        connectedDevice = null
    }

    // --- Key sending ---

    @SuppressLint("MissingPermission")
    private fun sendKeyboard(data: ByteArray) {
        val device = connectedDevice ?: return
        try {
            hidDevice?.sendReport(device, HidReportDescriptor.REPORT_ID_KEYBOARD.toInt(), data)
        } catch (_: SecurityException) {
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendConsumer(data: ByteArray) {
        val device = connectedDevice ?: return
        try {
            hidDevice?.sendReport(device, HidReportDescriptor.REPORT_ID_CONSUMER.toInt(), data)
        } catch (_: SecurityException) {
        }
    }

    /** Presses then releases a keyboard key (e.g. arrows, Enter, Escape). */
    fun tapKey(usage: Int, scope: CoroutineScope) {
        scope.launch {
            sendKeyboard(HidReportDescriptor.keyboardReport(usage))
            delay(40)
            sendKeyboard(HidReportDescriptor.keyboardReport(null))
        }
    }

    /** Presses then releases a consumer-control usage (volume, mute, power, home, back). */
    fun tapConsumer(usage: Int, scope: CoroutineScope) {
        scope.launch {
            sendConsumer(HidReportDescriptor.consumerReport(usage))
            delay(40)
            sendConsumer(HidReportDescriptor.consumerReport(0))
        }
    }
}
