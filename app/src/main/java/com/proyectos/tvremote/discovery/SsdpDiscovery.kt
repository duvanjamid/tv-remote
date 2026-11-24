package com.proyectos.tvremote.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.util.regex.Pattern

/**
 * SSDP/UPnP discovery for webOS (LG) TVs on the local network.
 *
 * LG webOS TVs answer M-SEARCH with LOCATION pointing to a UPnP device
 * description XML. We broadcast a generic ssdp:all search (works across
 * router/AP setups more reliably than a single exact search target) and
 * then fetch each LOCATION to confirm it's an LG/webOS device.
 */
object SsdpDiscovery {

    private const val SSDP_ADDRESS = "239.255.255.250"
    private const val SSDP_PORT = 1900

    private val searchTargets = listOf(
        "urn:lge:com:service:webos:second-screen-1",
        "urn:schemas-upnp-org:device:MediaRenderer:1",
        "ssdp:all"
    )

    suspend fun discover(timeoutMs: Long = 4000): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        val locations = mutableSetOf<String>()
        withTimeoutOrNull(timeoutMs) {
            sendSearchesAndCollect(locations, timeoutMs)
        }

        val client = OkHttpClient()
        val results = mutableListOf<DiscoveredDevice>()
        val seenHosts = mutableSetOf<String>()

        for (location in locations) {
            try {
                val uri = java.net.URI(location)
                val host = uri.host ?: continue
                if (!seenHosts.add(host)) continue

                val request = Request.Builder().url(location).build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: return@use
                    val isLg = body.contains("LG Electronics", ignoreCase = true) ||
                        body.contains("webos", ignoreCase = true) ||
                        body.contains("second-screen", ignoreCase = true)
                    if (isLg) {
                        val friendlyName = extractTag(body, "friendlyName") ?: "LG webOS TV"
                        results += DiscoveredDevice(
                            id = "webos:$host",
                            name = friendlyName,
                            host = host,
                            port = 3000,
                            type = DeviceType.WEBOS
                        )
                    }
                }
            } catch (_: Exception) {
                // ignore unreachable / malformed device description
            }
        }
        results
    }

    private fun sendSearchesAndCollect(locations: MutableSet<String>, timeoutMs: Long) {
        val socket = DatagramSocket(InetSocketAddress(0))
        socket.broadcast = true
        socket.soTimeout = 400
        try {
            val group = InetAddress.getByName(SSDP_ADDRESS)

            for (st in searchTargets) {
                val msg = buildString {
                    append("M-SEARCH * HTTP/1.1\r\n")
                    append("HOST: $SSDP_ADDRESS:$SSDP_PORT\r\n")
                    append("MAN: \"ssdp:discover\"\r\n")
                    append("MX: 2\r\n")
                    append("ST: $st\r\n")
                    append("\r\n")
                }
                val data = msg.toByteArray()
                try {
                    socket.send(DatagramPacket(data, data.size, group, SSDP_PORT))
                } catch (_: Exception) {
                }
            }

            val deadline = System.currentTimeMillis() + timeoutMs
            val buf = ByteArray(4096)
            while (System.currentTimeMillis() < deadline) {
                try {
                    val packet = DatagramPacket(buf, buf.size)
                    socket.receive(packet)
                    val text = String(packet.data, 0, packet.length)
                    extractHeader(text, "LOCATION")?.let { locations.add(it) }
                } catch (_: java.net.SocketTimeoutException) {
                    // expected, keep looping until deadline
                } catch (_: Exception) {
                    break
                }
            }
        } finally {
            socket.close()
        }
    }

    private fun extractHeader(text: String, header: String): String? {
        val pattern = Pattern.compile("$header:\\s*(.+)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group(1)?.trim() else null
    }

    private fun extractTag(xml: String, tag: String): String? {
        val pattern = Pattern.compile("<$tag>(.*?)</$tag>", Pattern.DOTALL)
        val matcher = pattern.matcher(xml)
        return if (matcher.find()) matcher.group(1)?.trim() else null
    }

    /** Best effort: list local IPv4 addresses, useful for debugging/logging. */
    fun localAddresses(): List<String> {
        val result = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                for (addr in iface.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr.address.size == 4) {
                        result += addr.hostAddress ?: continue
                    }
                }
            }
        } catch (_: Exception) {
        }
        return result
    }
}
