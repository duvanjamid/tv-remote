package com.proyectos.tvremote.discovery

import org.json.JSONArray
import org.json.JSONObject

/** Persists every TV ever discovered so the list screen opens populated, without a re-scan. */
object DeviceCacheCodec {

    fun toJson(devices: List<DiscoveredDevice>): String {
        val arr = JSONArray()
        devices.forEach { d ->
            arr.put(JSONObject().apply {
                put("id", d.id)
                put("name", d.name)
                put("host", d.host)
                put("port", d.port)
                put("type", d.type.name)
            })
        }
        return arr.toString()
    }

    fun fromJson(json: String): List<DiscoveredDevice> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val type = runCatching { DeviceType.valueOf(o.getString("type")) }.getOrNull() ?: return@mapNotNull null
            DiscoveredDevice(
                id = o.getString("id"),
                name = o.getString("name"),
                host = o.getString("host"),
                port = o.optInt("port", 0),
                type = type
            )
        }
    }.getOrDefault(emptyList())

    /** Merge freshly-scanned devices into the known list, replacing stale entries for the same host. */
    fun merge(known: List<DiscoveredDevice>, fresh: List<DiscoveredDevice>): List<DiscoveredDevice> {
        val byHost = known.associateBy { it.host }.toMutableMap()
        fresh.forEach { byHost[it.host] = it }
        return byHost.values.toList()
    }
}
