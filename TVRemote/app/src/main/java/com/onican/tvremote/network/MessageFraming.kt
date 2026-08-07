package com.onican.tvremote.network

import java.io.InputStream
import java.io.OutputStream
import com.google.protobuf.MessageLite

/**
 * The Android TV Remote protocol frames every protobuf message with a
 * varint length prefix (same varint encoding protobuf itself uses),
 * followed by the serialized message bytes.
 */
object MessageFraming {

    @Synchronized
    fun write(out: OutputStream, message: MessageLite) {
        val bytes = message.toByteArray()
        writeVarint(out, bytes.size)
        out.write(bytes)
        out.flush()
    }

    private fun writeVarint(out: OutputStream, value: Int) {
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

    /** Reads one length-prefixed frame and returns its raw bytes, or null on EOF. */
    fun readFrame(input: InputStream): ByteArray? {
        val length = readVarint(input) ?: return null
        val buffer = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(buffer, read, length - read)
            if (n < 0) return null
            read += n
        }
        return buffer
    }

    private fun readVarint(input: InputStream): Int? {
        var result = 0
        var shift = 0
        while (true) {
            val b = input.read()
            if (b < 0) return null
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) return result
            shift += 7
        }
    }
}
