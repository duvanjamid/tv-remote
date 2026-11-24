package com.proyectos.tvremote.androidtv

import com.proyectos.tvremote.proto.RemoteConfigure
import com.proyectos.tvremote.proto.RemoteDeviceInfo
import com.proyectos.tvremote.proto.RemoteDirection
import com.proyectos.tvremote.proto.RemoteKeyCode
import com.proyectos.tvremote.proto.RemoteKeyInject
import com.proyectos.tvremote.proto.RemoteMessage
import com.proyectos.tvremote.proto.RemotePingResponse
import com.proyectos.tvremote.proto.RemoteSetActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import javax.net.ssl.SSLSocket

sealed class AndroidTvRemoteState {
    object Connecting : AndroidTvRemoteState()
    object Ready : AndroidTvRemoteState()
    data class Disconnected(val reason: String) : AndroidTvRemoteState()
}

/**
 * Android TV Remote Service v2, remote-control channel (port 6466).
 * Reuses the client certificate established during pairing - the TV
 * identifies this device by that certificate, there's no separate token.
 */
class AndroidTvRemoteClient(
    private val host: String,
    private val identity: ClientIdentity,
    private val scope: CoroutineScope,
    private val port: Int = 6466
) {
    // PING | KEY | POWER | VOLUME | APP_LINK (no IME/voice - not needed for a basic remote)
    private val desiredFeatures = 1 or 2 or 32 or 64 or 512
    private var activeFeatures = desiredFeatures

    private lateinit var socket: SSLSocket
    private lateinit var input: InputStream
    private lateinit var output: OutputStream
    private var readJob: Job? = null

    var onStateChanged: ((AndroidTvRemoteState) -> Unit)? = null

    private fun setState(s: AndroidTvRemoteState) = onStateChanged?.invoke(s)

    suspend fun connect() {
        setState(AndroidTvRemoteState.Connecting)
        try {
            withContext(Dispatchers.IO) {
                val sslContext = AndroidTvCertificateStore.buildSslContext(identity)
                val s = sslContext.socketFactory.createSocket() as SSLSocket
                s.connect(InetSocketAddress(host, port), 5000)
                s.startHandshake()
                socket = s
                input = s.inputStream
                output = s.outputStream
            }
            readJob = scope.launch(Dispatchers.IO) { readLoop() }
        } catch (e: Exception) {
            setState(AndroidTvRemoteState.Disconnected(e.message ?: "error de conexión"))
        }
    }

    private fun readLoop() {
        try {
            while (true) {
                val bytes = Varint.readMessage(input) ?: break
                val msg = RemoteMessage.parseFrom(bytes)
                handle(msg)
            }
            setState(AndroidTvRemoteState.Disconnected("conexión cerrada"))
        } catch (e: Exception) {
            setState(AndroidTvRemoteState.Disconnected(e.message ?: "error de lectura"))
        }
    }

    private fun handle(msg: RemoteMessage) {
        when {
            msg.hasRemoteConfigure() -> {
                val serverCode1 = msg.remoteConfigure.code1
                activeFeatures = desiredFeatures and serverCode1
                val reply = RemoteMessage.newBuilder()
                    .setRemoteConfigure(
                        RemoteConfigure.newBuilder()
                            .setCode1(activeFeatures)
                            .setDeviceInfo(
                                RemoteDeviceInfo.newBuilder()
                                    .setUnknown1(1)
                                    .setUnknown2("1")
                                    .setPackageName("tvremote")
                                    .setAppVersion("1.0.0")
                                    .build()
                            )
                            .build()
                    )
                    .build()
                send(reply)
            }

            msg.hasRemoteSetActive() -> {
                val reply = RemoteMessage.newBuilder()
                    .setRemoteSetActive(RemoteSetActive.newBuilder().setActive(activeFeatures).build())
                    .build()
                send(reply)
            }

            msg.hasRemoteStart() -> {
                if (msg.remoteStart.started) {
                    setState(AndroidTvRemoteState.Ready)
                }
            }

            msg.hasRemotePingRequest() -> {
                val reply = RemoteMessage.newBuilder()
                    .setRemotePingResponse(
                        RemotePingResponse.newBuilder().setVal1(msg.remotePingRequest.val1).build()
                    )
                    .build()
                send(reply)
            }

            else -> {
                // ignore IME/voice/volume-info/app-link messages we don't act on yet
            }
        }
    }

    /** direction defaults to SHORT (a normal tap); use START_LONG/END_LONG for a held press. */
    fun sendKey(keyCode: RemoteKeyCode, direction: RemoteDirection = RemoteDirection.SHORT) {
        val msg = RemoteMessage.newBuilder()
            .setRemoteKeyInject(
                RemoteKeyInject.newBuilder()
                    .setKeyCode(keyCode)
                    .setDirection(direction)
                    .build()
            )
            .build()
        send(msg)
    }

    private fun send(msg: RemoteMessage) {
        try {
            synchronized(output) {
                Varint.writeMessage(output, msg.toByteArray())
            }
        } catch (_: Exception) {
            setState(AndroidTvRemoteState.Disconnected("error de escritura"))
        }
    }

    fun close() {
        readJob?.cancel()
        try {
            socket.close()
        } catch (_: Exception) {
        }
    }
}
