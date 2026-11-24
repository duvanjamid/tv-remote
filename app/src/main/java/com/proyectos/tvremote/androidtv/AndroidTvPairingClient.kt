package com.proyectos.tvremote.androidtv

import com.proyectos.tvremote.proto.PoloProto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import javax.net.ssl.SSLSocket

/**
 * Android TV Remote Service v2 pairing (port 6467), Polo protocol.
 * Wire reference: tronikos/androidtvremote2 (pairing.py), MIT-licensed
 * reverse-engineering of Google's protocol.
 */
class AndroidTvPairingClient(
    private val host: String,
    private val identity: ClientIdentity,
    private val port: Int = 6467
) {
    private lateinit var socket: SSLSocket
    private lateinit var input: InputStream
    private lateinit var output: OutputStream
    private lateinit var serverCert: X509Certificate

    sealed class Result {
        object AwaitingPin : Result()
        object Success : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun connect() = withContext(Dispatchers.IO) {
        val sslContext = AndroidTvCertificateStore.buildSslContext(identity)
        val s = sslContext.socketFactory.createSocket() as SSLSocket
        s.connect(InetSocketAddress(host, port), 5000)
        s.startHandshake()
        serverCert = s.session.peerCertificates[0] as X509Certificate
        socket = s
        input = s.inputStream
        output = s.outputStream
    }

    /** Runs the pairing_request / options / configuration exchange. After this, the TV shows a 6-digit PIN. */
    suspend fun startPairingUntilPinShown(): Result = withContext(Dispatchers.IO) {
        try {
            send(baseMessage().apply {
                pairingRequest = PoloProto.PairingRequest.newBuilder()
                    .setServiceName("atvremote")
                    .setClientName("TV Remote Android")
                    .build()
            }.build())

            val ack = receive() ?: return@withContext Result.Error("TV cerró la conexión")
            if (!ack.hasPairingRequestAck()) return@withContext Result.Error("Respuesta inesperada (sin pairing_request_ack)")

            val hexEncoding = PoloProto.Options.Encoding.newBuilder()
                .setType(PoloProto.Options.Encoding.EncodingType.ENCODING_TYPE_HEXADECIMAL)
                .setSymbolLength(6)
                .build()

            send(baseMessage().apply {
                options = PoloProto.Options.newBuilder()
                    .addInputEncodings(hexEncoding)
                    .setPreferredRole(PoloProto.Options.RoleType.ROLE_TYPE_INPUT)
                    .build()
            }.build())

            val serverOptions = receive() ?: return@withContext Result.Error("TV cerró la conexión")
            if (!serverOptions.hasOptions()) return@withContext Result.Error("Respuesta inesperada (sin options)")

            send(baseMessage().apply {
                configuration = PoloProto.Configuration.newBuilder()
                    .setEncoding(hexEncoding)
                    .setClientRole(PoloProto.Options.RoleType.ROLE_TYPE_INPUT)
                    .build()
            }.build())

            val configAck = receive() ?: return@withContext Result.Error("TV cerró la conexión")
            if (!configAck.hasConfigurationAck()) return@withContext Result.Error("Respuesta inesperada (sin configuration_ack)")

            Result.AwaitingPin
        } catch (e: Exception) {
            Result.Error(e.message ?: "error de conexión")
        }
    }

    /** Call once the user types the 6-digit hex PIN shown on the TV. */
    suspend fun submitPin(pin: String): Result = withContext(Dispatchers.IO) {
        val cleaned = pin.trim()
        if (cleaned.length != 6 || !cleaned.matches(Regex("^[0-9a-fA-F]{6}$"))) {
            return@withContext Result.Error("PIN debe ser 6 caracteres hex")
        }
        try {
            val (clientMod, clientExp) = modulusAndExponent(identity.certificate)
            val (serverMod, serverExp) = modulusAndExponent(serverCert)

            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(unsignedMinimalBytes(clientMod))
            digest.update(unsignedMinimalBytes(clientExp))
            digest.update(unsignedMinimalBytes(serverMod))
            digest.update(unsignedMinimalBytes(serverExp))
            digest.update(hexToBytes(cleaned.substring(2)))
            val hash = digest.digest()

            val expectedFirstByte = cleaned.substring(0, 2).toInt(16)
            if ((hash[0].toInt() and 0xFF) != expectedFirstByte) {
                return@withContext Result.Error("PIN incorrecto")
            }

            send(baseMessage().apply {
                secret = PoloProto.Secret.newBuilder()
                    .setSecret(com.google.protobuf.ByteString.copyFrom(hash))
                    .build()
            }.build())

            val ack = receive() ?: return@withContext Result.Error("TV cerró la conexión")
            if (ack.hasSecretAck()) Result.Success else Result.Error("PIN rechazado por la TV")
        } catch (e: Exception) {
            Result.Error(e.message ?: "error validando PIN")
        }
    }

    fun close() {
        try {
            socket.close()
        } catch (_: Exception) {
        }
    }

    private fun baseMessage(): PoloProto.OuterMessage.Builder =
        PoloProto.OuterMessage.newBuilder()
            .setProtocolVersion(2)
            .setStatus(PoloProto.OuterMessage.Status.STATUS_OK)

    private fun send(message: PoloProto.OuterMessage) {
        Varint.writeMessage(output, message.toByteArray())
    }

    private fun receive(): PoloProto.OuterMessage? {
        val bytes = Varint.readMessage(input) ?: return null
        return PoloProto.OuterMessage.parseFrom(bytes)
    }

    private fun modulusAndExponent(cert: X509Certificate): Pair<BigInteger, BigInteger> {
        val pub = cert.publicKey as RSAPublicKey
        return pub.modulus to pub.publicExponent
    }

    /** Big-endian, minimal length, no sign byte - matches Python's int.to_bytes((bit_length+7)//8, "big"). */
    private fun unsignedMinimalBytes(v: BigInteger): ByteArray {
        val bytes = v.toByteArray()
        return if (bytes.size > 1 && bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
    }

    private fun hexToBytes(hex: String): ByteArray {
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val index = i * 2
            out[i] = ((Character.digit(hex[index], 16) shl 4) + Character.digit(hex[index + 1], 16)).toByte()
        }
        return out
    }
}
