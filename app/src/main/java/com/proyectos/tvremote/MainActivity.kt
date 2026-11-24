package com.proyectos.tvremote

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.proyectos.tvremote.adb.AdbTvBoxClient
import com.proyectos.tvremote.androidtv.AndroidTvCertificateStore
import com.proyectos.tvremote.androidtv.AndroidTvPairingClient
import com.proyectos.tvremote.androidtv.AndroidTvRemoteClient
import com.proyectos.tvremote.androidtv.AndroidTvRemoteState
import com.proyectos.tvremote.bluetooth.BluetoothHidController
import com.proyectos.tvremote.bluetooth.BluetoothHidState
import com.proyectos.tvremote.bluetooth.HidReportDescriptor
import com.proyectos.tvremote.discovery.DeviceCacheCodec
import com.proyectos.tvremote.discovery.DeviceType
import com.proyectos.tvremote.discovery.DiscoveredDevice
import com.proyectos.tvremote.discovery.NsdDiscovery
import com.proyectos.tvremote.discovery.SsdpDiscovery
import com.proyectos.tvremote.proto.RemoteDirection
import com.proyectos.tvremote.proto.RemoteKeyCode
import com.proyectos.tvremote.storage.PrefsStore
import com.proyectos.tvremote.ui.AdbScreen
import com.proyectos.tvremote.ui.AdbUiState
import com.proyectos.tvremote.ui.AndroidTvScreen
import com.proyectos.tvremote.ui.AndroidTvUiState
import com.proyectos.tvremote.ui.BluetoothScreen
import com.proyectos.tvremote.ui.DeviceListScreen
import com.proyectos.tvremote.ui.DeviceSettingsDialog
import com.proyectos.tvremote.ui.WebOsScreen
import com.proyectos.tvremote.ui.theme.TvRemoteTheme
import com.proyectos.tvremote.webos.PairingState
import com.proyectos.tvremote.webos.VolumeStatus
import com.proyectos.tvremote.webos.WakeOnLan
import com.proyectos.tvremote.webos.WebOsApp
import com.proyectos.tvremote.webos.WebOsCacheCodec
import com.proyectos.tvremote.webos.WebOsChannel
import com.proyectos.tvremote.webos.WebOsClient
import com.proyectos.tvremote.webos.WebOsInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private sealed class Screen {
    object DeviceList : Screen()
    data class WebOs(val device: DiscoveredDevice) : Screen()
    data class AndroidTv(val device: DiscoveredDevice) : Screen()
    object Bluetooth : Screen()
    object Adb : Screen()
}

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ignore result, scan proceeds best-effort */ }

    private val bluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            onBluetoothPermissionResult?.invoke(results.values.all { it })
        }

    private val bluetoothActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { onBluetoothActivityResult?.invoke() }

    private var onBluetoothPermissionResult: ((Boolean) -> Unit)? = null
    private var onBluetoothActivityResult: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        val prefsStore = PrefsStore(this)

        setContent {
            var themeMode by remember { mutableStateOf(prefsStore.getThemeMode()) }
            TvRemoteTheme(themeMode = themeMode) {
                androidx.compose.material3.Surface {
                    App(
                        prefsStore = prefsStore,
                        themeMode = themeMode,
                        onThemeModeChange = { themeMode = it }
                    )
                }
            }
        }
    }

    @Composable
    private fun App(prefsStore: PrefsStore, themeMode: String, onThemeModeChange: (String) -> Unit) {
        var screen by remember { mutableStateOf<Screen>(Screen.DeviceList) }
        // Open populated from every TV ever found, instead of empty until a fresh scan finishes.
        var devices by remember {
            mutableStateOf(DeviceCacheCodec.fromJson(prefsStore.getKnownDevicesJson() ?: "[]"))
        }
        var scanning by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        fun scan() {
            if (scanning) return
            scanning = true
            scope.launch {
                val results = mutableListOf<DiscoveredDevice>()
                try {
                    results += SsdpDiscovery.discover()
                } catch (_: Exception) {
                }
                try {
                    results += NsdDiscovery.discover(this@MainActivity)
                } catch (_: Exception) {
                }
                val fresh = results.distinctBy { it.id }
                devices = DeviceCacheCodec.merge(devices, fresh)
                prefsStore.setKnownDevicesJson(DeviceCacheCodec.toJson(devices))
                scanning = false
            }
        }

        when (val current = screen) {
            is Screen.DeviceList -> DeviceListScreen(
                devices = devices,
                scanning = scanning,
                onScan = { scan() },
                onSelect = { device ->
                    screen = when (device.type) {
                        DeviceType.WEBOS -> Screen.WebOs(device)
                        DeviceType.ANDROID_TV, DeviceType.CAST_ONLY, DeviceType.ANDROID_ADB -> Screen.AndroidTv(device)
                    }
                },
                onAddBluetooth = { screen = Screen.Bluetooth },
                onAddAdb = { screen = Screen.Adb }
            )

            is Screen.WebOs -> WebOsScreenHost(
                device = current.device,
                prefsStore = prefsStore,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                onBack = { screen = Screen.DeviceList }
            )

            is Screen.AndroidTv -> AndroidTvScreenHost(
                device = current.device,
                onBack = { screen = Screen.DeviceList }
            )

            is Screen.Bluetooth -> BluetoothScreenHost(
                onBack = { screen = Screen.DeviceList }
            )

            is Screen.Adb -> AdbScreenHost(
                prefsStore = prefsStore,
                onBack = { screen = Screen.DeviceList },
                onConnected = { device ->
                    devices = DeviceCacheCodec.merge(devices, listOf(device))
                    prefsStore.setKnownDevicesJson(DeviceCacheCodec.toJson(devices))
                    screen = Screen.AndroidTv(device)
                }
            )
        }
    }

    @Composable
    private fun WebOsScreenHost(
        device: DiscoveredDevice,
        prefsStore: PrefsStore,
        themeMode: String,
        onThemeModeChange: (String) -> Unit,
        onBack: () -> Unit
    ) {
        val scope = rememberCoroutineScope()
        val client = remember(device.host) { WebOsClient(device.host, prefsStore) }
        var state by remember { mutableStateOf<PairingState>(PairingState.Idle) }
        var volume by remember { mutableStateOf<VolumeStatus?>(null) }

        // Seed every tab from cache immediately (instant open), then silently refresh from the TV.
        var apps by remember {
            mutableStateOf(WebOsCacheCodec.appsFromJson(prefsStore.getCache("apps", device.host) ?: "[]"))
        }
        var appsLoading by remember { mutableStateOf(false) }
        var inputs by remember {
            mutableStateOf(WebOsCacheCodec.inputsFromJson(prefsStore.getCache("inputs", device.host) ?: "[]"))
        }
        var inputsLoading by remember { mutableStateOf(false) }
        var channels by remember {
            mutableStateOf(WebOsCacheCodec.channelsFromJson(prefsStore.getCache("channels", device.host) ?: "[]"))
        }
        var channelsLoading by remember { mutableStateOf(false) }
        var recentApps by remember {
            mutableStateOf(WebOsCacheCodec.appsFromJson(prefsStore.getRecentAppsJson(device.host) ?: "[]"))
        }

        fun recordRecentApp(app: WebOsApp) {
            val stamped = app.copy(launchedAt = System.currentTimeMillis())
            recentApps = (listOf(stamped) + recentApps.filter { it.id != app.id }).take(8)
            prefsStore.setRecentAppsJson(device.host, WebOsCacheCodec.appsToJson(recentApps))
        }
        var favoriteIds by remember { mutableStateOf(prefsStore.getFavoriteAppIds(device.host)) }
        fun toggleFavorite(app: WebOsApp) {
            favoriteIds = if (app.id in favoriteIds) favoriteIds - app.id else favoriteIds + app.id
            prefsStore.setFavoriteAppIds(device.host, favoriteIds)
        }
        var showSettings by remember { mutableStateOf(false) }
        var displayName by remember { mutableStateOf(prefsStore.getCustomName(device.host) ?: device.name) }

        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

        DisposableEffect(Unit) {
            if (prefsStore.getKeepScreenOn()) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            onDispose { window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }

        fun refreshApps(showSpinner: Boolean) {
            if (showSpinner) appsLoading = true
            scope.launch {
                try {
                    val fresh = client.listApps()
                    if (fresh.isNotEmpty()) {
                        apps = fresh
                        prefsStore.setCache("apps", device.host, WebOsCacheCodec.appsToJson(fresh))
                    }
                } catch (_: Exception) {
                }
                appsLoading = false
            }
        }

        fun refreshInputs(showSpinner: Boolean) {
            if (showSpinner) inputsLoading = true
            scope.launch {
                try {
                    val fresh = client.listInputs()
                    if (fresh.isNotEmpty()) {
                        inputs = fresh
                        prefsStore.setCache("inputs", device.host, WebOsCacheCodec.inputsToJson(fresh))
                    }
                } catch (_: Exception) {
                }
                inputsLoading = false
            }
        }

        fun refreshChannels(showSpinner: Boolean) {
            if (showSpinner) channelsLoading = true
            scope.launch {
                try {
                    val fresh = client.listChannels()
                    if (fresh.isNotEmpty()) {
                        channels = fresh
                        prefsStore.setCache("channels", device.host, WebOsCacheCodec.channelsToJson(fresh))
                    }
                } catch (_: Exception) {
                }
                channelsLoading = false
            }
        }

        LaunchedEffect(device.host) {
            client.onStateChanged = { s ->
                state = s
                if (s is PairingState.Paired) {
                    client.observeVolume { volume = it }

                    // Auto-discover the TV's own MAC straight from the TV - the user never types it.
                    if (prefsStore.getMacAddresses(device.host).isEmpty()) {
                        scope.launch {
                            try {
                                val macs = client.getNetworkMacs()
                                if (macs.isNotEmpty()) prefsStore.setMacAddresses(device.host, macs)
                            } catch (_: Exception) {
                            }
                        }
                    }

                    // Background-sync every cached list so it's fresh next time, without blocking the UI.
                    refreshApps(showSpinner = apps.isEmpty())
                    refreshInputs(showSpinner = inputs.isEmpty())
                    refreshChannels(showSpinner = channels.isEmpty())
                }
            }
            client.connectAndPair()
        }

        WebOsScreen(
            host = device.host,
            displayName = displayName,
            state = state,
            volume = volume,
            apps = apps,
            appsLoading = appsLoading,
            favoriteIds = favoriteIds,
            recentApps = recentApps,
            inputs = inputs,
            inputsLoading = inputsLoading,
            channels = channels,
            channelsLoading = channelsLoading,
            onBack = {
                client.close()
                onBack()
            },
            onButton = { name ->
                if (prefsStore.getHapticsEnabled()) {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                }
                scope.launch {
                    when (name) {
                        "CHANNELUP" -> client.requestFireAndForget("ssap://tv/channelUp")
                        "CHANNELDOWN" -> client.requestFireAndForget("ssap://tv/channelDown")
                        else -> client.button(name)
                    }
                }
            },
            onPowerOff = {
                client.requestFireAndForget("ssap://system/turnOff")
            },
            onPowerOn = {
                val macs = prefsStore.getMacAddresses(device.host)
                if (macs.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "Todavía no tengo la MAC de esta TV. Conectate una vez con la TV encendida.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    scope.launch {
                        val result = WakeOnLan.sendAll(macs)
                        val msg = if (result.isSuccess) "Señal de encendido enviada" else "Error: ${result.exceptionOrNull()?.message}"
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onRetry = {
                scope.launch { client.connectAndPair() }
            },
            onToggleMute = { client.setMute(volume?.muted != true) },
            onLaunchApp = { app ->
                client.launchApp(app.id)
                recordRecentApp(app)
            },
            onToggleFavorite = { app -> toggleFavorite(app) },
            onSwitchInput = { input -> client.switchInput(input.id) },
            onSwitchChannel = { channel -> client.switchChannel(channel.id) },
            onLoadApps = { refreshApps(showSpinner = apps.isEmpty()) },
            onLoadInputs = { refreshInputs(showSpinner = inputs.isEmpty()) },
            onLoadChannels = { refreshChannels(showSpinner = channels.isEmpty()) },
            onOpenStore = {
                // There's no public SSAP API to search/install Store apps remotely - this just
                // opens LG Content Store on the TV itself so the user finishes it with our d-pad.
                client.launchApp("com.webos.app.discovery")
            },
            onSendToast = { client.sendToast("📱 Hola desde el celu!") },
            onSendText = { text -> client.sendText(text) },
            onSendEnter = { client.sendEnterKey() },
            onMove = { dx, dy -> scope.launch { client.move(dx, dy) } },
            onOpenSettings = { showSettings = true }
        )

        if (showSettings) {
            var keepScreenOn by remember { mutableStateOf(prefsStore.getKeepScreenOn()) }
            var hapticsEnabled by remember { mutableStateOf(prefsStore.getHapticsEnabled()) }

            DeviceSettingsDialog(
                currentName = displayName,
                discoveredMacs = prefsStore.getMacAddresses(device.host),
                themeMode = themeMode,
                keepScreenOn = keepScreenOn,
                hapticsEnabled = hapticsEnabled,
                onDismiss = { showSettings = false },
                onSave = { name ->
                    if (name.isNotBlank()) {
                        prefsStore.setCustomName(device.host, name)
                        displayName = name
                    }
                    showSettings = false
                },
                onThemeModeChange = { mode ->
                    onThemeModeChange(mode)
                    prefsStore.setThemeMode(mode)
                },
                onKeepScreenOnChange = { value ->
                    keepScreenOn = value
                    prefsStore.setKeepScreenOn(value)
                },
                onHapticsChange = { value ->
                    hapticsEnabled = value
                    prefsStore.setHapticsEnabled(value)
                },
                onForgetDevice = {
                    client.close()
                    prefsStore.forgetDevice(device.host)
                    showSettings = false
                    onBack()
                }
            )
        }
    }

    @Composable
    private fun AndroidTvScreenHost(device: DiscoveredDevice, onBack: () -> Unit) {
        val scope = rememberCoroutineScope()
        val isAdb = device.type == DeviceType.ANDROID_ADB
        var uiState by remember { mutableStateOf<AndroidTvUiState>(AndroidTvUiState.Connecting()) }
        var pairingClient by remember { mutableStateOf<AndroidTvPairingClient?>(null) }
        var remoteClient by remember { mutableStateOf<AndroidTvRemoteClient?>(null) }
        val adbClient = remember(isAdb) { if (isAdb) AdbTvBoxClient(this@MainActivity) else null }
        val prefsStore = remember { PrefsStore(this@MainActivity) }

        fun startRemote() {
            val identity = AndroidTvCertificateStore.getOrCreate(this@MainActivity, device.host)
            val rc = AndroidTvRemoteClient(device.host, identity, scope)
            rc.onStateChanged = { s ->
                uiState = when (s) {
                    is AndroidTvRemoteState.Connecting -> AndroidTvUiState.Connecting()
                    is AndroidTvRemoteState.Ready -> AndroidTvUiState.Remote
                    is AndroidTvRemoteState.Disconnected -> AndroidTvUiState.Error(s.reason)
                }
            }
            remoteClient = rc
            scope.launch { rc.connect() }
        }

        fun startPairingFlow() {
            uiState = AndroidTvUiState.Connecting()
            val identity = AndroidTvCertificateStore.getOrCreate(this@MainActivity, device.host)
            val pc = AndroidTvPairingClient(device.host, identity)
            pairingClient = pc
            scope.launch {
                pc.connect()
                val result = pc.startPairingUntilPinShown()
                uiState = when (result) {
                    is AndroidTvPairingClient.Result.AwaitingPin -> AndroidTvUiState.AwaitingPin()
                    is AndroidTvPairingClient.Result.Error -> AndroidTvUiState.Error(result.message)
                    is AndroidTvPairingClient.Result.Success -> AndroidTvUiState.Remote
                }
            }
        }

        fun startAdb() {
            uiState = AndroidTvUiState.Connecting()
            scope.launch {
                adbClient?.connect(device.host, device.port) { s ->
                    uiState = when (s) {
                        is AdbUiState.EnterAddress, is AdbUiState.Connecting -> AndroidTvUiState.Connecting()
                        is AdbUiState.AwaitingAuthorization ->
                            AndroidTvUiState.Connecting("Aceptá el popup de depuración en la TV (intento ${s.attempt})...")
                        is AdbUiState.Remote -> AndroidTvUiState.Remote
                        is AdbUiState.Error -> AndroidTvUiState.Error(s.message)
                    }
                }
            }
        }

        LaunchedEffect(device.host) {
            when {
                isAdb -> startAdb()
                prefsStore.isAndroidTvPaired(device.host) -> startRemote()
                else -> startPairingFlow()
            }
        }

        AndroidTvScreen(
            host = device.host,
            uiState = uiState,
            onBack = {
                pairingClient?.close()
                remoteClient?.close()
                adbClient?.close()
                onBack()
            },
            onSubmitPin = { pin ->
                scope.launch {
                    val result = pairingClient?.submitPin(pin)
                    when (result) {
                        is AndroidTvPairingClient.Result.Success -> {
                            prefsStore.setAndroidTvPaired(device.host, true)
                            pairingClient?.close()
                            startRemote()
                        }
                        is AndroidTvPairingClient.Result.Error -> {
                            uiState = AndroidTvUiState.AwaitingPin(error = result.message)
                        }
                        else -> {}
                    }
                }
            },
            onKey = { name ->
                if (isAdb) {
                    scope.launch { adbClient?.sendKeyEvent("KEYCODE_$name") }
                } else {
                    val code = keyCodeFor(name)
                    if (code != null) remoteClient?.sendKey(code, RemoteDirection.SHORT)
                }
            },
            onRetry = { if (isAdb) startAdb() else startPairingFlow() }
        )
    }

    @Composable
    private fun BluetoothScreenHost(onBack: () -> Unit) {
        val scope = rememberCoroutineScope()
        val controller = remember { BluetoothHidController(this@MainActivity) }
        var state by remember { mutableStateOf<BluetoothHidState>(BluetoothHidState.Registering) }

        fun ensurePermissionsThenRegister() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val needed = listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
                    .filter {
                        androidx.core.content.ContextCompat.checkSelfPermission(this@MainActivity, it) !=
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                    }
                if (needed.isNotEmpty()) {
                    onBluetoothPermissionResult = { granted ->
                        if (granted) controller.register()
                        else state = BluetoothHidState.Error("Se necesita permiso de Bluetooth para este modo")
                    }
                    bluetoothPermissionLauncher.launch(needed.toTypedArray())
                    return
                }
            }
            controller.register()
        }

        DisposableEffect(Unit) {
            controller.onStateChanged = { state = it }
            onDispose { controller.close() }
        }

        LaunchedEffect(Unit) { ensurePermissionsThenRegister() }

        val connected = state as? BluetoothHidState.Connected
        if (connected != null) {
            // Same control pad certified/ADB devices use - only the key-sending backend differs.
            AndroidTvScreen(
                host = connected.deviceName,
                uiState = AndroidTvUiState.Remote,
                onBack = { controller.close(); onBack() },
                onSubmitPin = {},
                onKey = { name -> controller.sendTvKey(name, scope) },
                onRetry = {}
            )
        } else {
            BluetoothScreen(
                state = state,
                onBack = {
                    controller.close()
                    onBack()
                },
                onEnableBluetooth = {
                    onBluetoothActivityResult = { ensurePermissionsThenRegister() }
                    bluetoothActivityLauncher.launch(controller.enableBluetoothIntent())
                },
                onMakeDiscoverable = {
                    onBluetoothActivityResult = {}
                    bluetoothActivityLauncher.launch(controller.discoverableIntent())
                },
                onRetry = { ensurePermissionsThenRegister() }
            )
        }
    }

    @Composable
    private fun AdbScreenHost(prefsStore: PrefsStore, onBack: () -> Unit, onConnected: (DiscoveredDevice) -> Unit) {
        val scope = rememberCoroutineScope()
        // Just a connectivity/auth probe for the add-device form - once it reaches Remote, the
        // real control session is opened fresh by AndroidTvScreenHost's own AdbTvBoxClient.
        val client = remember { AdbTvBoxClient(this@MainActivity) }
        var uiState by remember { mutableStateOf<AdbUiState>(AdbUiState.EnterAddress) }
        var connectJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

        DisposableEffect(Unit) { onDispose { client.close() } }

        fun startConnect(host: String, port: Int) {
            prefsStore.setAdbLastAddress(host, port)
            connectJob?.cancel()
            connectJob = scope.launch {
                client.connect(host, port) { s ->
                    uiState = s
                    if (s is AdbUiState.Remote) {
                        client.close()
                        onConnected(
                            DiscoveredDevice(
                                id = "adb:$host:$port",
                                name = "TV box ADB ($host)",
                                host = host,
                                port = port,
                                type = DeviceType.ANDROID_ADB
                            )
                        )
                    }
                }
            }
        }

        AdbScreen(
            uiState = uiState,
            initialHost = prefsStore.getAdbLastHost() ?: "",
            initialPort = prefsStore.getAdbLastPort().toString(),
            onBack = {
                connectJob?.cancel()
                client.close()
                onBack()
            },
            onConnect = { host, port -> startConnect(host, port) },
            onCancel = {
                connectJob?.cancel()
                client.close()
                uiState = AdbUiState.EnterAddress
            },
            onRetry = { uiState = AdbUiState.EnterAddress }
        )
    }

    private fun keyCodeFor(name: String): RemoteKeyCode? = when (name) {
        "HOME" -> RemoteKeyCode.KEYCODE_HOME
        "BACK" -> RemoteKeyCode.KEYCODE_BACK
        "MENU" -> RemoteKeyCode.KEYCODE_MENU
        "POWER" -> RemoteKeyCode.KEYCODE_POWER
        "DPAD_UP" -> RemoteKeyCode.KEYCODE_DPAD_UP
        "DPAD_DOWN" -> RemoteKeyCode.KEYCODE_DPAD_DOWN
        "DPAD_LEFT" -> RemoteKeyCode.KEYCODE_DPAD_LEFT
        "DPAD_RIGHT" -> RemoteKeyCode.KEYCODE_DPAD_RIGHT
        "DPAD_CENTER" -> RemoteKeyCode.KEYCODE_DPAD_CENTER
        "VOLUME_UP" -> RemoteKeyCode.KEYCODE_VOLUME_UP
        "VOLUME_DOWN" -> RemoteKeyCode.KEYCODE_VOLUME_DOWN
        "VOLUME_MUTE" -> RemoteKeyCode.KEYCODE_VOLUME_MUTE
        else -> null
    }

    // Maps AndroidTvPad's key names onto this phone's HID keyboard/consumer usages.
    // MENU has no consumer-control usage in our descriptor, so it's a no-op over Bluetooth.
    private fun BluetoothHidController.sendTvKey(name: String, scope: CoroutineScope) {
        when (name) {
            "DPAD_UP" -> tapKey(HidReportDescriptor.Key.UP_ARROW, scope)
            "DPAD_DOWN" -> tapKey(HidReportDescriptor.Key.DOWN_ARROW, scope)
            "DPAD_LEFT" -> tapKey(HidReportDescriptor.Key.LEFT_ARROW, scope)
            "DPAD_RIGHT" -> tapKey(HidReportDescriptor.Key.RIGHT_ARROW, scope)
            "DPAD_CENTER" -> tapKey(HidReportDescriptor.Key.ENTER, scope)
            "BACK" -> tapKey(HidReportDescriptor.Key.ESCAPE, scope)
            "HOME" -> tapConsumer(HidReportDescriptor.Consumer.AC_HOME, scope)
            "VOLUME_UP" -> tapConsumer(HidReportDescriptor.Consumer.VOLUME_UP, scope)
            "VOLUME_DOWN" -> tapConsumer(HidReportDescriptor.Consumer.VOLUME_DOWN, scope)
            "VOLUME_MUTE" -> tapConsumer(HidReportDescriptor.Consumer.MUTE, scope)
            "POWER" -> tapConsumer(HidReportDescriptor.Consumer.POWER, scope)
            else -> {}
        }
    }
}
