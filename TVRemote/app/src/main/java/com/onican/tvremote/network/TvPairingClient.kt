package com.onican.tvremote.network

import android.content.Context
import com.onican.tvremote.proto.EncodingType
import com.onican.tvremote.proto.PairingConfiguration
import com.onican.tvremote.proto.PairingEncoding
import com.onican.tvremote.proto.PairingMessage
import com.onican.tvremote.proto.PairingOption
import com.onican.tvremote.proto.PairingRequest
import com.onican.tvremote.proto.PairingSecret
import com.onican.tvremote.proto.RoleType
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import javax.net.ssl.SSLSocket

class PairingException(message: String) : Exception(message)

/**
 * Handles the full pairing flow with an Android TV / Google TV device on
 * port 6467: TLS handshake with a self-signed client certificate, then a
 * short protobuf exchange that ends with the TV showing a 6-digit code on
 * screen, which the user types back into [submitCode].
 */
class TvPairingClient(context: Context, private val host: String, private val port: Int = 6467) {

    private val certManager = TvCertificateManager(context)
    private val trustManager = CapturingTrustManager()
    private lateinit var socket: SSLSocket

    /** Connects, performs the request/option/configuration exchange, and returns
     * once the TV is expected to be showing a pairing code on screen. */
    fun startPairing(clientName: String = "TV Remote") {
        socket = TvSslContext.openSocket(host, port, certManager.keyStore, "tvremote".toCharArray(), trustManager)

        send(
            PairingMessage.newBuilder()
                .setProtocolVersion("2")
                .setStatus(200)
                .setPairingRequest(
                    PairingRequest.newBuilder()
                        .setServiceName("TVRemoteApp")
                        .setClientName(clientName)
                        .build()
                )
                .build()
        )
        receive() // PairingRequestAck

        val hexEncoding = PairingEncoding.newBuilder()
            .setType(EncodingType.ENCODING_TYPE_HEXADECIMAL)
            .setSymbolLength(6)
            .build()

        send(
            PairingMessage.newBuilder()
                .setProtocolVersion("2")
                .setStatus(200)
                .setPairingOption(
                    PairingOption.newBuilder()
                        .setInputEncodings(hexEncoding)
                        .setOutputEncodings(hexEncoding)
                        .setPreferredRole(false)
                        .build()
                )
                .build()
        )
        receive() // PairingOptionAck

        send(
            PairingMessage.newBuilder()
                .setProtocolVersion("2")
                .setStatus(200)
                .setPairingConfiguration(
                    PairingConfiguration.newBuilder()
                        .setEncoding(hexEncoding)
                        .setClientRole(RoleType.ROLE_TYPE_INPUT)
                        .build()
                )
                .build()
        )
        receive() // PairingConfigurationAck -> TV now shows the code
    }

    /**
     * Submits the 6-digit hex code shown on the TV screen. Returns true if
     * pairing completed successfully; the client certificate can then be
     * reused for the remote-control connection on port 6466.
     */
    fun submitCode(code: String): Boolean {
        val clientCert = certManager.clientCertificate
        val serverCert = trustManager.serverCertificate
            ?: throw PairingException("Sertifikat TV belum diterima")

        val hash = computeSecretHash(clientCert, serverCert, code)
        val checkByte = hexToBytes(code.substring(0, 2))[0]
        if (hash[0] != checkByte) {
            throw PairingException("Kode salah")
        }

        send(
            PairingMessage.newBuilder()
                .setProtocolVersion("2")
                .setStatus(200)
                .setPairingSecret(
                    PairingSecret.newBuilder()
                        .setSecret(com.google.protobuf.ByteString.copyFrom(hash))
                        .build()
                )
                .build()
        )
        val ack = receive()
        return ack.hasPairingSecretAck()
    }

    fun close() {
        try {
            socket.close()
        } catch (_: Exception) {
        }
    }

    private fun computeSecretHash(clientCert: X509Certificate, serverCert: X509Certificate, code: String): ByteArray {
        val clientPub = clientCert.publicKey as RSAPublicKey
        val serverPub = serverCert.publicKey as RSAPublicKey

        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(clientPub.modulus.abs().toByteArray())
        digest.update(clientPub.publicExponent.abs().toByteArray())
        digest.update(serverPub.modulus.abs().toByteArray())
        digest.update(serverPub.publicExponent.abs().toByteArray())
        // First byte (2 hex chars) of the code is a checksum; the remaining
        // two bytes (4 hex chars) are hashed in.
        digest.update(hexToBytes(code.substring(2, 6)))
        return digest.digest()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val idx = i * 2
            out[i] = ((Character.digit(hex[idx], 16) shl 4) + Character.digit(hex[idx + 1], 16)).toByte()
        }
        return out
    }

    private fun send(message: PairingMessage) {
        MessageFraming.write(socket.outputStream, message)
    }

    private fun receive(): PairingMessage {
        val bytes = MessageFraming.readFrame(socket.inputStream)
            ?: throw PairingException("Koneksi ke TV terputus")
        return PairingMessage.parseFrom(bytes)
    }
}
