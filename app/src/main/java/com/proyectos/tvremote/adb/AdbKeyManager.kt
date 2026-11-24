package com.proyectos.tvremote.adb

import android.content.Context
import dadb.AdbKeyPair
import java.io.File

/**
 * Persists this app's own ADB RSA keypair in app-private storage, the same role
 * ~/.android/adbkey plays for desktop adb. Reusing the same keypair across runs means a TV box
 * that already authorized it won't show the "Allow debugging?" popup again.
 */
object AdbKeyManager {
    fun getOrCreateKeyPair(context: Context): AdbKeyPair {
        val dir = File(context.filesDir, "adb")
        dir.mkdirs()
        val privateKeyFile = File(dir, "adbkey")
        val publicKeyFile = File(dir, "adbkey.pub")
        if (!privateKeyFile.exists()) {
            AdbKeyPair.generate(privateKeyFile, publicKeyFile)
        }
        return AdbKeyPair.read(privateKeyFile, publicKeyFile)
    }
}
