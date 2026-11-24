package com.proyectos.tvremote.storage

import android.content.Context

/**
 * Simple per-device persistence:
 *  - webOS SSAP client-key (so we don't re-pair every launch)
 *  - Android TV Remote v2 pairing state + where its cert/key PEM files live
 */
class PrefsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("tvremote_prefs", Context.MODE_PRIVATE)

    fun getWebOsClientKey(host: String): String? = prefs.getString("webos_key_$host", null)

    fun setWebOsClientKey(host: String, key: String) {
        prefs.edit().putString("webos_key_$host", key).apply()
    }

    fun isAndroidTvPaired(host: String): Boolean = prefs.getBoolean("atv_paired_$host", false)

    fun setAndroidTvPaired(host: String, paired: Boolean) {
        prefs.edit().putBoolean("atv_paired_$host", paired).apply()
    }

    /** Auto-discovered via ssap://com.webos.service.connectionmanager/getinfo - no manual typing needed. */
    fun getMacAddresses(host: String): List<String> =
        prefs.getString("macs_$host", null)?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    fun setMacAddresses(host: String, macs: List<String>) {
        prefs.edit().putString("macs_$host", macs.filter { it.isNotBlank() }.joinToString(",")).apply()
    }

    fun getCustomName(host: String): String? = prefs.getString("name_$host", null)

    fun setCustomName(host: String, name: String) {
        prefs.edit().putString("name_$host", name).apply()
    }

    // Generic JSON cache (apps/inputs/channels lists) so the UI has something
    // to show instantly while a fresh copy is fetched from the TV in the background.
    fun getCache(key: String, host: String): String? = prefs.getString("cache_${key}_$host", null)

    fun setCache(key: String, host: String, json: String) {
        prefs.edit().putString("cache_${key}_$host", json).apply()
    }

    /** Every TV ever discovered, so the list screen opens populated instead of empty until re-scanned. */
    fun getKnownDevicesJson(): String? = prefs.getString("known_devices", null)

    fun setKnownDevicesJson(json: String) {
        prefs.edit().putString("known_devices", json).apply()
    }

    fun getFavoriteAppIds(host: String): Set<String> =
        prefs.getStringSet("fav_apps_$host", emptySet()) ?: emptySet()

    fun setFavoriteAppIds(host: String, ids: Set<String>) {
        prefs.edit().putStringSet("fav_apps_$host", ids).apply()
    }

    // Global app preferences (not per-TV).
    fun getThemeMode(): String = prefs.getString("theme_mode", "system") ?: "system"
    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun getKeepScreenOn(): Boolean = prefs.getBoolean("keep_screen_on", true)
    fun setKeepScreenOn(value: Boolean) {
        prefs.edit().putBoolean("keep_screen_on", value).apply()
    }

    fun getHapticsEnabled(): Boolean = prefs.getBoolean("haptics_enabled", true)
    fun setHapticsEnabled(value: Boolean) {
        prefs.edit().putBoolean("haptics_enabled", value).apply()
    }

    // Last IP/port typed in the "Agregar por ADB" screen, so it's prefilled next time.
    fun getAdbLastHost(): String? = prefs.getString("adb_last_host", null)
    fun getAdbLastPort(): Int = prefs.getInt("adb_last_port", 5555)

    fun setAdbLastAddress(host: String, port: Int) {
        prefs.edit().putString("adb_last_host", host).putInt("adb_last_port", port).apply()
    }

    // Recently launched apps per TV - webOS/Android TV expose no "continue watching" API
    // (that's internal to each streaming app), so this is our own "recently opened from
    // this remote" history, most-recent-first, capped at 10.
    fun getRecentAppsJson(host: String): String? = prefs.getString("recents_$host", null)

    fun setRecentAppsJson(host: String, json: String) {
        prefs.edit().putString("recents_$host", json).apply()
    }

    fun forgetDevice(host: String) {
        prefs.edit()
            .remove("webos_key_$host")
            .remove("atv_paired_$host")
            .remove("macs_$host")
            .remove("name_$host")
            .remove("cache_apps_$host")
            .remove("cache_inputs_$host")
            .remove("cache_channels_$host")
            .remove("fav_apps_$host")
            .remove("recents_$host")
            .apply()
    }
}
