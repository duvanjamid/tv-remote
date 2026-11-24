package com.proyectos.tvremote.androidtv

import android.content.Context
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class ClientIdentity(val privateKey: PrivateKey, val certificate: X509Certificate)

/**
 * Generates (and persists) a self-signed RSA-2048 client certificate per TV,
 * mirroring tronikos/androidtvremote2's certificate_generator.py so the TV
 * can recognize this "device" across reconnects after pairing once.
 *
 * There's no stock Android API to build an X.509 cert, so this uses
 * Bouncy Castle (bcpkix/bcprov), same as most Android mTLS client-cert apps.
 */
object AndroidTvCertificateStore {

    init {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    private const val PREFS = "tvremote_atv_certs"
    private const val CLIENT_NAME = "TV Remote Android"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getOrCreate(context: Context, host: String): ClientIdentity {
        val p = prefs(context)
        val certB64 = p.getString("cert_$host", null)
        val keyB64 = p.getString("key_$host", null)
        if (certB64 != null && keyB64 != null) {
            try {
                val certBytes = Base64.getDecoder().decode(certB64)
                val keyBytes = Base64.getDecoder().decode(keyB64)
                val cf = CertificateFactory.getInstance("X.509")
                val cert = cf.generateCertificate(certBytes.inputStream()) as X509Certificate
                val keySpec = java.security.spec.PKCS8EncodedKeySpec(keyBytes)
                val privateKey = java.security.KeyFactory.getInstance("RSA").generatePrivate(keySpec)
                return ClientIdentity(privateKey, cert)
            } catch (_: Exception) {
                // fall through and regenerate if stored data is corrupt
            }
        }

        val identity = generate()
        p.edit()
            .putString("cert_$host", Base64.getEncoder().encodeToString(identity.certificate.encoded))
            .putString("key_$host", Base64.getEncoder().encodeToString(identity.privateKey.encoded))
            .apply()
        return identity
    }

    private fun generate(): ClientIdentity {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPair = keyPairGenerator.generateKeyPair()

        val name = X500Name("CN=$CLIENT_NAME")
        val now = Date()
        val notAfter = Date(now.time + 10L * 365 * 24 * 60 * 60 * 1000)
        val serial = BigInteger.valueOf(1000)

        val certBuilder: X509v3CertificateBuilder = JcaX509v3CertificateBuilder(
            name, serial, now, notAfter, name, keyPair.public
        )
        certBuilder.addExtension(Extension.basicConstraints, false, BasicConstraints(0))
        certBuilder.addExtension(
            Extension.subjectAlternativeName, false,
            GeneralNames(GeneralName(GeneralName.dNSName, CLIENT_NAME))
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(keyPair.private)
        val holder = certBuilder.build(signer)
        val cert = JcaX509CertificateConverter().setProvider("BC").getCertificate(holder)

        return ClientIdentity(keyPair.private, cert)
    }

    /** Client-cert (mutual) TLS context: presents our identity, trusts any server cert (self-signed TV). */
    fun buildSslContext(identity: ClientIdentity): SSLContext {
        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(null, null)
        keyStore.setKeyEntry(
            "client",
            identity.privateKey,
            charArrayOf(),
            arrayOf<Certificate>(identity.certificate)
        )

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, charArrayOf())

        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmf.keyManagers, arrayOf<TrustManager>(trustAll), null)
        return sslContext
    }
}
