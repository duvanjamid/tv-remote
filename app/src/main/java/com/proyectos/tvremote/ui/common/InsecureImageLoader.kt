package com.proyectos.tvremote.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * App/input icons are served straight off the TV itself over HTTPS with its
 * own self-signed certificate (e.g. https://<tv-ip>:3001/resources/...).
 * Coil's default OkHttp client rejects that (untrusted cert), so icons
 * silently fail to load. This loader trusts it, same as our SSAP socket does.
 */
@Composable
fun rememberInsecureImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), SecureRandom())

        val okHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .build()

        ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .build()
    }
}
