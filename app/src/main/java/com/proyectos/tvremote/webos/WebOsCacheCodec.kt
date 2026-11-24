package com.proyectos.tvremote.webos

import org.json.JSONArray
import org.json.JSONObject

/** Tiny JSON (de)serializers so app/input/channel lists can be cached offline in SharedPreferences. */
object WebOsCacheCodec {

    fun appsToJson(apps: List<WebOsApp>): String {
        val arr = JSONArray()
        apps.forEach { app ->
            arr.put(JSONObject().apply {
                put("id", app.id)
                put("title", app.title)
                put("iconUrl", app.iconUrl ?: "")
                put("launchedAt", app.launchedAt)
            })
        }
        return arr.toString()
    }

    fun appsFromJson(json: String): List<WebOsApp> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            WebOsApp(
                id = o.getString("id"),
                title = o.getString("title"),
                iconUrl = o.optString("iconUrl", "").ifEmpty { null },
                launchedAt = o.optLong("launchedAt", 0L)
            )
        }
    }.getOrDefault(emptyList())

    fun inputsToJson(inputs: List<WebOsInput>): String {
        val arr = JSONArray()
        inputs.forEach { input ->
            arr.put(JSONObject().apply {
                put("id", input.id)
                put("label", input.label)
                put("iconUrl", input.iconUrl ?: "")
                put("connected", input.connected)
            })
        }
        return arr.toString()
    }

    fun inputsFromJson(json: String): List<WebOsInput> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            WebOsInput(
                id = o.getString("id"),
                label = o.getString("label"),
                iconUrl = o.optString("iconUrl", "").ifEmpty { null },
                connected = o.optBoolean("connected", false)
            )
        }
    }.getOrDefault(emptyList())

    fun channelsToJson(channels: List<WebOsChannel>): String {
        val arr = JSONArray()
        channels.forEach { ch ->
            arr.put(JSONObject().apply {
                put("id", ch.id)
                put("number", ch.number)
                put("name", ch.name)
                put("isRadio", ch.isRadio)
            })
        }
        return arr.toString()
    }

    fun channelsFromJson(json: String): List<WebOsChannel> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            WebOsChannel(
                id = o.getString("id"),
                number = o.getString("number"),
                name = o.getString("name"),
                isRadio = o.optBoolean("isRadio", false)
            )
        }
    }.getOrDefault(emptyList())

    /** Moves `app` to the front of `current` (de-duped by id), capped to `max` entries. */
    fun withRecentApp(current: List<WebOsApp>, app: WebOsApp, max: Int = 10): List<WebOsApp> =
        (listOf(app) + current.filterNot { it.id == app.id }).take(max)
}
