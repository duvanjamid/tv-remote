package com.proyectos.tvremote.discovery

enum class DeviceType {
    WEBOS,
    ANDROID_TV,
    CAST_ONLY,
    // Generic TV box controlled over plain ADB shell (no SSAP, no Android TV Remote v2 cert).
    ANDROID_ADB
}

data class DiscoveredDevice(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val type: DeviceType
)
