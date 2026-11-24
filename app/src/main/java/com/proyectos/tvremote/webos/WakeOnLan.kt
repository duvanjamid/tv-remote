package com.proyectos.tvremote.webos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Sends a Wake-on-LAN magic packet so a webOS (or any WOL-capable) TV can be
 * turned on from standby over WiFi/LAN. Requires:
 *  - The TV's MAC address (shown in Settings > Network > Wi-Fi/Wired details).
 *  - "Turn on via WiFi"/"Quick Start+"/"Wake on LAN" enabled on the TV, since
 *    the TV's network chip must stay listening for this packet while asleep.
 */
object WakeOnLan {

    /** Sends to every known MAC (wired + wifi) since we don't know which interface is currently listening. */
    suspend fun sendAll(macs: List<String>): Result<Unit> {
        if (macs.isEmpty()) return Result.failure(IllegalStateException("Sin MAC guardada todavía"))
        var lastError: Throwable? = null
        var anySuccess = false
        for (mac in macs) {
            send(mac).onSuccess { anySuccess = true }.onFailure { lastError = it }
        }
        return if (anySuccess) Result.success(Unit) else Result.failure(lastError ?: IllegalStateException("fallo desconocido"))
    }

    suspend fun send(mac: String, broadcastAddress: String = "255.255.255.255", port: Int = 9): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val macBytes = parseMac(mac)
                val packet = ByteArray(6 + 16 * 6).also { buf ->
                    for (i in 0 until 6) buf[i] = 0xFF.toByte()
                    for (i in 6 until buf.size step 6) {
                        System.arraycopy(macBytes, 0, buf, i, 6)
                    }
                }
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    val address = InetAddress.getByName(broadcastAddress)
                    socket.send(DatagramPacket(packet, packet.size, address, port))
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun parseMac(mac: String): ByteArray {
        val clean = mac.trim().replace("-", ":").split(":")
        require(clean.size == 6) { "MAC inválida: usá formato AA:BB:CC:DD:EE:FF" }
        return ByteArray(6) { i -> clean[i].toInt(16).toByte() }
    }

    fun isValidMac(mac: String): Boolean =
        mac.trim().matches(Regex("^([0-9a-fA-F]{2}[:-]){5}[0-9a-fA-F]{2}$"))
}
