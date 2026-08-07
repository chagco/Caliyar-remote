package com.onican.tvremote.network

import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

/**
 * The TV's certificate is self-signed and unknown ahead of time, exactly like
 * the official Google TV app: trust is established the first time by the user
 * confirming the pairing code shown on screen, not by a CA. This trust manager
 * accepts any server certificate but records it so the pairing secret (which
 * is derived from both certificates) can be computed.
 */
class CapturingTrustManager : X509TrustManager {
    var serverCertificate: X509Certificate? = null
        private set

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        serverCertificate = chain?.get(0)
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
}

object TvSslContext {

    fun openSocket(host: String, port: Int, keyStore: KeyStore, password: CharArray, trustManager: CapturingTrustManager): SSLSocket {
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, password)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmf.keyManagers, arrayOf(trustManager), SecureRandom())

        val socket = sslContext.socketFactory.createSocket(host, port) as SSLSocket
        socket.soTimeout = 15000
        socket.startHandshake()
        return socket
    }
}
