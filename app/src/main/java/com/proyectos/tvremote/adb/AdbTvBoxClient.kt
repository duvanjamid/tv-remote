package com.proyectos.tvremote.adb

import android.content.Context
import com.proyectos.tvremote.ui.AdbUiState
import dadb.Dadb
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * ADB-over-network control for generic Android TV boxes that don't speak SSAP or Android TV
 * Remote v2 (not mDNS-discoverable, ports 6466/6467 closed) but have USB/wireless debugging
 * enabled. The phone itself is the ADB client straight to the box's adbd on the network - no PC,
 * no root. Built on dadb (pure Kotlin/JVM ADB client, same one Maestro uses).
 *
 * The first connect triggers adbd's "Allow debugging?" popup on the TV, which only resolves once
 * the user taps it - so we keep retrying the handshake (same pattern desktop adb's "waiting for
 * device" uses) instead of blocking on a single read forever.
 */
class AdbTvBoxClient(context: Context) {
    private val appContext = context.applicationContext

    @Volatile
    private var dadb: Dadb? = null

    suspend fun connect(host: String, port: Int, onState: (AdbUiState) -> Unit) {
        withContext(Dispatchers.IO) {
            close()
            val keyPair = AdbKeyManager.getOrCreateKeyPair(appContext)
            var attempt = 0
            while (attempt < MAX_ATTEMPTS) {
                attempt++
                onState(if (attempt == 1) AdbUiState.Connecting else AdbUiState.AwaitingAuthorization(attempt - 1))

                val candidate = Dadb.create(
                    host = host,
                    port = port,
                    keyPair = keyPair,
                    connectTimeout = CONNECT_TIMEOUT_MS,
                    socketTimeout = SOCKET_TIMEOUT_MS
                )
                try {
                    candidate.shell("echo tvremote_ok")
                    dadb = candidate
                    onState(AdbUiState.Remote)
                    return@withContext
                } catch (e: CancellationException) {
                    candidate.close()
                    throw e
                } catch (e: Exception) {
                    candidate.close()
                    delay(RETRY_DELAY_MS)
                }
            }
            onState(
                AdbUiState.Error(
                    "No se pudo conectar. Verificá la IP/puerto y que aceptaste el popup de depuración en la TV."
                )
            )
        }
    }

    suspend fun sendKeyEvent(keyCode: String) {
        val current = dadb ?: return
        withContext(Dispatchers.IO) {
            try {
                current.shell("input keyevent $keyCode")
            } catch (_: Exception) {
                // Best-effort: a dropped connection surfaces on the user's next tap too.
            }
        }
    }

    fun close() {
        dadb?.close()
        dadb = null
    }

    companion object {
        private const val MAX_ATTEMPTS = 30
        private const val CONNECT_TIMEOUT_MS = 5000
        private const val SOCKET_TIMEOUT_MS = 6000
        private const val RETRY_DELAY_MS = 1200L
    }
}
