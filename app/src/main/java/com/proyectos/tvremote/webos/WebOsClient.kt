package com.proyectos.tvremote.webos

import com.proyectos.tvremote.storage.PrefsStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

data class WebOsApp(val id: String, val title: String, val iconUrl: String?, val launchedAt: Long = 0L)
data class WebOsInput(val id: String, val label: String, val iconUrl: String?, val connected: Boolean)
data class WebOsChannel(val id: String, val number: String, val name: String, val isRadio: Boolean = false)
data class VolumeStatus(val volume: Int, val muted: Boolean)

sealed class PairingState {
    object Idle : PairingState()
    object Connecting : PairingState()
    /** TV is showing the "Allow this app to connect?" prompt - user must accept on the TV remote. */
    object AwaitingPrompt : PairingState()
    object Paired : PairingState()
    data class Failed(val reason: String) : PairingState()
}

/**
 * SSAP (webOS Second Screen Application Protocol) client.
 *
 * Wire format reference: hobbyquaker/lgtv2 (MIT) - plain JSON frames
 * {id, type, uri, payload} over a websocket, plus a "specialized socket"
 * (newline key:value frames, not JSON) for pointer/dpad/button input.
 */
class WebOsClient(
    private val host: String,
    private val store: PrefsStore
) {
    private var webSocket: WebSocket? = null
    private var pointerSocket: WebSocket? = null

    /**
     * SSAP quirk: "register" (like "subscribe") can send MULTIPLE responses for
     * the same request id over time - first an ack while the TV shows the
     * "allow this app?" prompt (payload has no client-key yet), then later the
     * real result once the user accepts/rejects on the TV remote. A plain
     * "request" only ever gets one response. We must not resolve the register
     * deferred on that first, empty ack.
     */
    private data class Pending(val deferred: CompletableDeferred<JSONObject>, val isRegister: Boolean)
    private val pending = ConcurrentHashMap<String, Pending>()

    /** "subscribe" frames keep delivering updates on the same id forever (volume, current app, etc). */
    private val subscriptions = ConcurrentHashMap<String, (JSONObject) -> Unit>()
    private var cidCounter = 0

    var state: PairingState = PairingState.Idle
        private set
    var onStateChanged: ((PairingState) -> Unit)? = null
        set(value) {
            field = value
        }

    private fun setState(s: PairingState) {
        state = s
        onStateChanged?.invoke(s)
    }

    private fun nextCid(): String = "req_${cidCounter++}_${System.currentTimeMillis()}"

    private fun trustAllClient(): OkHttpClient {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), SecureRandom())
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .readTimeout(0, TimeUnit.MILLISECONDS) // websocket: no read timeout
            .pingInterval(15, TimeUnit.SECONDS)
            .build()
    }

    /** Connects (trying wss:3001 then ws:3000) and runs pairing. Suspends until Paired or Failed. */
    suspend fun connectAndPair(): PairingState {
        setState(PairingState.Connecting)
        val client = trustAllClient()
        val urls = listOf("wss://$host:3001", "ws://$host:3000")

        var connected = false
        for (url in urls) {
            val opened = CompletableDeferred<Boolean>()
            val socket = client.newWebSocket(
                Request.Builder().url(url).build(),
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        if (!opened.isCompleted) opened.complete(true)
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        handleFrame(text)
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        if (!opened.isCompleted) opened.complete(false)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        if (this@WebOsClient.webSocket === webSocket) {
                            this@WebOsClient.webSocket = null
                        }
                    }
                }
            )
            val ok = try {
                withTimeout(2500) { opened.await() }
            } catch (_: Exception) {
                false
            }
            if (ok) {
                webSocket = socket
                connected = true
                break
            } else {
                socket.cancel()
            }
        }

        if (!connected) {
            setState(PairingState.Failed("No se pudo conectar a $host (puertos 3000/3001)"))
            return state
        }

        return try {
            val response = withTimeout(60_000) { register() }
            val key = response.optString("client-key", "")
            if (key.isNotEmpty()) {
                store.setWebOsClientKey(host, key)
                setState(PairingState.Paired)
            } else {
                setState(PairingState.Failed("Respuesta sin client-key"))
            }
            state
        } catch (e: Exception) {
            setState(PairingState.Failed(e.message ?: "timeout esperando aceptar en la TV"))
            state
        }
    }

    private fun manifest(): JSONObject {
        val permissions = JSONArray(
            listOf(
                "LAUNCH", "LAUNCH_WEBAPP", "APP_TO_APP", "CLOSE", "TEST_OPEN", "TEST_PROTECTED",
                "CONTROL_AUDIO", "CONTROL_DISPLAY", "CONTROL_INPUT_JOYSTICK",
                "CONTROL_INPUT_MEDIA_RECORDING", "CONTROL_INPUT_MEDIA_PLAYBACK", "CONTROL_INPUT_TV",
                "CONTROL_POWER", "READ_APP_STATUS", "READ_CURRENT_CHANNEL", "READ_INPUT_DEVICE_LIST",
                "READ_NETWORK_STATE", "READ_RUNNING_APPS", "READ_TV_CHANNEL_LIST",
                "WRITE_NOTIFICATION_TOAST", "READ_POWER_STATE", "READ_COUNTRY_INFO", "READ_SETTINGS",
                "CONTROL_TV_SCREEN", "CONTROL_TV_STANBY", "CONTROL_FAVORITE_GROUP", "CONTROL_USER_INFO",
                "CHECK_BLUETOOTH_DEVICE", "CONTROL_BLUETOOTH", "CONTROL_TIMER_INFO",
                "STB_INTERNAL_CONNECTION", "CONTROL_RECORDING", "READ_RECORDING_STATE",
                "WRITE_RECORDING_LIST", "READ_RECORDING_LIST", "READ_RECORDING_SCHEDULE",
                "WRITE_RECORDING_SCHEDULE", "READ_STORAGE_DEVICE_LIST", "READ_TV_PROGRAM_INFO",
                "CONTROL_BOX_CHANNEL", "READ_TV_ACR_AUTH_TOKEN", "READ_TV_CONTENT_STATE",
                "READ_TV_CURRENT_TIME", "ADD_LAUNCHER_CHANNEL", "SET_CHANNEL_SKIP",
                "RELEASE_CHANNEL_SKIP", "CONTROL_CHANNEL_BLOCK", "DELETE_SELECT_CHANNEL",
                "CONTROL_CHANNEL_GROUP", "SCAN_TV_CHANNELS", "CONTROL_TV_POWER", "CONTROL_WOL",
                "CONTROL_INPUT_TEXT", "CONTROL_MOUSE_AND_KEYBOARD",
                "READ_INSTALLED_APPS", "READ_LGE_SDX", "READ_NOTIFICATIONS", "SEARCH",
                "WRITE_SETTINGS", "WRITE_NOTIFICATION_ALERT", "READ_UPDATE_INFO",
                "UPDATE_FROM_REMOTE_APP", "READ_LGE_TV_INPUT_EVENTS", "TEST_SECURE"
            )
        )
        return JSONObject().apply {
            put("manifestVersion", 1)
            put("appVersion", "1.0")
            put("permissions", permissions)
        }
    }

    private suspend fun register(): JSONObject {
        val deferred = CompletableDeferred<JSONObject>()
        val cid = nextCid()
        pending[cid] = Pending(deferred, isRegister = true)

        val payload = JSONObject().apply {
            put("forcePairing", false)
            put("pairingType", "PROMPT")
            put("manifest", manifest())
            store.getWebOsClientKey(host)?.let { put("client-key", it) }
        }
        val frame = JSONObject().apply {
            put("id", cid)
            put("type", "register")
            put("payload", payload)
        }
        webSocket?.send(frame.toString())
        setState(PairingState.AwaitingPrompt)
        return deferred.await()
    }

    /** Fire-and-forget / awaited SSAP request, e.g. "ssap://audio/volumeUp". */
    suspend fun request(uri: String, payload: JSONObject = JSONObject(), timeoutMs: Long = 8000): JSONObject {
        val deferred = CompletableDeferred<JSONObject>()
        val cid = nextCid()
        pending[cid] = Pending(deferred, isRegister = false)
        val frame = JSONObject().apply {
            put("id", cid)
            put("type", "request")
            put("uri", uri)
            put("payload", payload)
        }
        webSocket?.send(frame.toString())
        return withTimeout(timeoutMs) { deferred.await() }
    }

    fun requestFireAndForget(uri: String, payload: JSONObject = JSONObject()) {
        val frame = JSONObject().apply {
            put("id", nextCid())
            put("type", "request")
            put("uri", uri)
            put("payload", payload)
        }
        webSocket?.send(frame.toString())
    }

    /** Subscribes to a live-updating SSAP endpoint (e.g. volume). Callback fires repeatedly. */
    fun subscribe(uri: String, callback: (JSONObject) -> Unit) {
        val cid = nextCid()
        subscriptions[cid] = callback
        val frame = JSONObject().apply {
            put("id", cid)
            put("type", "subscribe")
            put("uri", uri)
            put("payload", JSONObject())
        }
        webSocket?.send(frame.toString())
    }

    fun observeVolume(callback: (VolumeStatus) -> Unit) {
        subscribe("ssap://audio/getVolume") { payload ->
            callback(VolumeStatus(payload.optInt("volume", 0), payload.optBoolean("muted", false)))
        }
    }

    fun setVolume(level: Int) = requestFireAndForget("ssap://audio/setVolume", JSONObject().put("volume", level))
    fun setMute(muted: Boolean) = requestFireAndForget("ssap://audio/setMute", JSONObject().put("mute", muted))
    fun sendToast(message: String) =
        requestFireAndForget("ssap://system.notifications/createToast", JSONObject().put("message", message))
    fun launchApp(id: String) = requestFireAndForget("ssap://system.launcher/launch", JSONObject().put("id", id))
    fun switchInput(inputId: String) =
        requestFireAndForget("ssap://tv/switchInput", JSONObject().put("inputId", inputId))

    suspend fun listApps(): List<WebOsApp> {
        val payload = request("ssap://com.webos.applicationManager/listLaunchPoints", timeoutMs = 10_000)
        val arr = payload.optJSONArray("launchPoints") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optString("id", "")
            if (id.isEmpty()) return@mapNotNull null
            WebOsApp(
                id = id,
                title = o.optString("title", id),
                iconUrl = o.optString("icon", "").ifEmpty { o.optString("largeIcon", "").ifEmpty { null } }
            )
        }
    }

    suspend fun listInputs(): List<WebOsInput> {
        val payload = request("ssap://tv/getExternalInputList", timeoutMs = 10_000)
        val arr = payload.optJSONArray("devices") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optString("id", "")
            if (id.isEmpty()) return@mapNotNull null
            WebOsInput(
                id = id,
                label = o.optString("label", id),
                iconUrl = o.optString("icon", "").ifEmpty { null },
                connected = o.optBoolean("connected", false)
            )
        }
    }

    suspend fun listChannels(): List<WebOsChannel> {
        val payload = request("ssap://tv/getChannelList", timeoutMs = 10_000)
        val arr = payload.optJSONArray("channelList") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optString("channelId", "")
            if (id.isEmpty()) return@mapNotNull null
            WebOsChannel(
                id = id,
                number = o.optString("channelNumber", ""),
                name = o.optString("channelName", ""),
                isRadio = o.optBoolean("Radio", false)
            )
        }
    }

    fun switchChannel(channelId: String) =
        requestFireAndForget("ssap://tv/openChannel", JSONObject().put("channelId", channelId))

    /** Types into whatever text field is focused on the TV (search box, login password, etc). */
    fun sendText(text: String) =
        requestFireAndForget("ssap://com.webos.service.ime/insertText", JSONObject().put("text", text).put("replace", 0))

    fun sendEnterKey() = requestFireAndForget("ssap://com.webos.service.ime/sendEnterKey")
    fun sendDeleteKey() = requestFireAndForget("ssap://com.webos.service.ime/deleteCharacters", JSONObject().put("count", 1))

    /** Reads the TV's own WiFi/Ethernet MAC straight from the TV - no manual typing needed for Wake-on-LAN. */
    suspend fun getNetworkMacs(): List<String> {
        val payload = request("ssap://com.webos.service.connectionmanager/getinfo", timeoutMs = 8000)
        val wifi = payload.optJSONObject("wifiInfo")?.optString("macAddress", "")
        val wired = payload.optJSONObject("wiredInfo")?.optString("macAddress", "")
        return listOfNotNull(wired?.ifEmpty { null }, wifi?.ifEmpty { null })
    }

    /** Touchpad-style relative cursor move via the pointer socket. */
    suspend fun move(dx: Float, dy: Float) {
        if (!ensurePointerSocket()) return
        pointerSocket?.send("type:move\ndx:${dx.toInt()}\ndy:${dy.toInt()}\n\n")
    }

    suspend fun scroll(dx: Float, dy: Float) {
        if (!ensurePointerSocket()) return
        pointerSocket?.send("type:scroll\ndx:${dx.toInt()}\ndy:${dy.toInt()}\n\n")
    }

    private var pointerSocketReady = CompletableDeferred<Boolean>()

    /** Opens the special pointer/button input socket (plain key:value framing, not JSON). */
    suspend fun ensurePointerSocket(): Boolean {
        if (pointerSocket != null) return true
        return try {
            val res = request("ssap://com.webos.service.networkinput/getPointerInputSocket")
            val socketPath = res.optString("socketPath", "")
            if (socketPath.isEmpty()) return false

            val deferred = CompletableDeferred<Boolean>()
            val client = trustAllClient()
            pointerSocket = client.newWebSocket(
                Request.Builder().url(socketPath).build(),
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        if (!deferred.isCompleted) deferred.complete(true)
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        pointerSocket = null
                        if (!deferred.isCompleted) deferred.complete(false)
                    }
                }
            )
            withTimeout(4000) { deferred.await() }
        } catch (_: Exception) {
            false
        }
    }

    /** name e.g. "UP","DOWN","LEFT","RIGHT","ENTER","BACK","HOME","EXIT","MENU","VOLUMEUP","VOLUMEDOWN","MUTE" */
    suspend fun button(name: String) {
        if (!ensurePointerSocket()) return
        pointerSocket?.send("type:button\nname:$name\n\n")
    }

    suspend fun click() {
        if (!ensurePointerSocket()) return
        pointerSocket?.send("type:click\n\n")
    }

    private fun handleFrame(text: String) {
        val json = try {
            JSONObject(text)
        } catch (_: Exception) {
            return
        }
        val id = json.optString("id", "")

        subscriptions[id]?.let { callback ->
            val payload = json.optJSONObject("payload") ?: JSONObject()
            callback(payload)
            return
        }

        val entry = pending[id] ?: return
        val type = json.optString("type", "")

        if (type == "error") {
            pending.remove(id)
            entry.deferred.completeExceptionally(RuntimeException(json.optString("error", "SSAP error")))
            return
        }

        val payload = json.optJSONObject("payload") ?: JSONObject()

        if (payload.has("returnValue") && !payload.optBoolean("returnValue", true)) {
            pending.remove(id)
            entry.deferred.completeExceptionally(
                RuntimeException(payload.optString("errorText", "SSAP returnValue=false"))
            )
            return
        }

        if (entry.isRegister) {
            // First frame is just "prompt shown on TV, waiting for user" - payload
            // has no client-key yet. Keep waiting for the real follow-up frame
            // (same id) that arrives once the user accepts on the TV. Rejection
            // / failure arrives as a type:"error" frame, handled above.
            val hasKey = payload.optString("client-key", "").isNotEmpty()
            if (!hasKey) {
                return
            }
        }

        pending.remove(id)
        entry.deferred.complete(payload)
    }

    fun close() {
        pointerSocket?.cancel()
        webSocket?.cancel()
        pointerSocket = null
        webSocket = null
        subscriptions.clear()
        pending.clear()
    }
}
