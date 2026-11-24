package com.proyectos.tvremote.androidtv

import java.io.InputStream
import java.io.OutputStream

/**
 * Protobuf-style base-128 varint framing used by the Android TV Remote v2
 * protocol on both the pairing (6467) and remote (6466) TLS sockets:
 * each message is [varint length][protobuf body], length = body size in bytes.
 */
object Varint {

    fun writeMessage(out: OutputStream, body: ByteArray) {
        writeRaw(out, body.size)
        out.write(body)
        out.flush()
    }

    private fun writeRaw(out: OutputStream, value: Int) {
        var v = value
        while (true) {
            if (v and 0x7F.inv() == 0) {
                out.write(v)
                return
            } else {
                out.write((v and 0x7F) or 0x80)
                v = v ushr 7
            }
        }
    }

    /** Blocks until one full varint-prefixed message is read, or returns null on EOF. */
    fun readMessage(input: InputStream): ByteArray? {
        var result = 0
        var shift = 0
        while (true) {
            val b = input.read()
            if (b == -1) return null
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) break
            shift += 7
            if (shift > 35) throw java.io.IOException("Varint too long")
        }
        val len = result
        if (len == 0) return ByteArray(0)
        val buf = ByteArray(len)
        var read = 0
        while (read < len) {
            val n = input.read(buf, read, len - read)
            if (n == -1) return null
            read += n
        }
        return buf
    }
}
