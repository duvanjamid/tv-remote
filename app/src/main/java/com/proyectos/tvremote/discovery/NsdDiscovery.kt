package com.proyectos.tvremote.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * mDNS discovery for Android TV devices.
 *
 * "_androidtvremote2._tcp." is the official Android TV Remote Service (v2) -
 * used by the Google Home app / official remote. Devices advertising this
 * support full remote control (dpad, keys, volume, app links).
 *
 * "_googlecast._tcp." covers plain Chromecast / cast-only receivers, which
 * cannot be fully remote-controlled (no dpad), just shown for visibility.
 */
object NsdDiscovery {

    private const val TAG = "NsdDiscovery"
    const val SERVICE_TYPE_ANDROID_TV = "_androidtvremote2._tcp."
    const val SERVICE_TYPE_CAST = "_googlecast._tcp."

    suspend fun discover(context: Context, timeoutMs: Long = 4000): List<DiscoveredDevice> {
        val nsdManager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
        val found = ConcurrentLinkedQueue<DiscoveredDevice>()

        val androidTvDone = CompletableDeferred<Unit>()
        val castDone = CompletableDeferred<Unit>()

        val atvListener = makeListener(nsdManager, DeviceType.ANDROID_TV, found, androidTvDone)
        val castListener = makeListener(nsdManager, DeviceType.CAST_ONLY, found, castDone)

        try {
            nsdManager.discoverServices(SERVICE_TYPE_ANDROID_TV, NsdManager.PROTOCOL_DNS_SD, atvListener)
        } catch (e: Exception) {
            Log.w(TAG, "discoverServices androidtv failed", e)
            androidTvDone.complete(Unit)
        }
        try {
            nsdManager.discoverServices(SERVICE_TYPE_CAST, NsdManager.PROTOCOL_DNS_SD, castListener)
        } catch (e: Exception) {
            Log.w(TAG, "discoverServices cast failed", e)
            castDone.complete(Unit)
        }

        delay(timeoutMs)

        safeStop(nsdManager, atvListener)
        safeStop(nsdManager, castListener)

        // Dedupe: if a host is already known as ANDROID_TV (full remote control),
        // drop the CAST_ONLY entry for the same host.
        val list = found.toList()
        val fullControlHosts = list.filter { it.type == DeviceType.ANDROID_TV }.map { it.host }.toSet()
        return list.filterNot { it.type == DeviceType.CAST_ONLY && it.host in fullControlHosts }
            .distinctBy { it.id }
    }

    private fun makeListener(
        nsdManager: NsdManager,
        type: DeviceType,
        sink: ConcurrentLinkedQueue<DiscoveredDevice>,
        done: CompletableDeferred<Unit>
    ): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String?) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                try {
                    @Suppress("DEPRECATION")
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo?, errorCode: Int) {
                            Log.w(TAG, "resolve failed for ${info?.serviceName}: $errorCode")
                        }

                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val host = info.host?.hostAddress ?: return
                            val port = if (info.port > 0) info.port else 6467
                            sink += DiscoveredDevice(
                                id = "${type.name.lowercase()}:$host",
                                name = info.serviceName ?: host,
                                host = host,
                                port = port,
                                type = type
                            )
                        }
                    })
                } catch (e: Exception) {
                    Log.w(TAG, "resolveService threw", e)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {}
            override fun onDiscoveryStopped(serviceType: String?) {
                if (!done.isCompleted) done.complete(Unit)
            }

            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                if (!done.isCompleted) done.complete(Unit)
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                if (!done.isCompleted) done.complete(Unit)
            }
        }
    }

    private fun safeStop(nsdManager: NsdManager, listener: NsdManager.DiscoveryListener) {
        try {
            nsdManager.stopServiceDiscovery(listener)
        } catch (_: Exception) {
        }
    }
}
