package com.onican.tvremote.network

import android.content.Context
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import javax.security.auth.x500.X500Principal

/**
 * Generates (once) and persists a self-signed RSA client certificate used to
 * identify this app to the TV during pairing, and to authenticate the TLS
 * connections afterwards. This mirrors what the official Google TV app does:
 * pairing is certificate-based, not account/password-based.
 */
class TvCertificateManager(context: Context) {

    private val keystoreFile = File(context.filesDir, "tvremote_client.p12")
    private val password = "tvremote".toCharArray()
    private val alias = "client"

    val keyStore: KeyStore by lazy { loadOrCreate() }

    val clientCertificate: X509Certificate
        get() = keyStore.getCertificate(alias) as X509Certificate

    private fun loadOrCreate(): KeyStore {
        val ks = KeyStore.getInstance("PKCS12")
        if (keystoreFile.exists()) {
            FileInputStream(keystoreFile).use { ks.load(it, password) }
        } else {
            ks.load(null, null)
            val (keyPair, cert) = generateSelfSigned()
            ks.setKeyEntry(alias, keyPair.private, password, arrayOf(cert))
            FileOutputStream(keystoreFile).use { ks.store(it, password) }
        }
        return ks
    }

    private fun generateSelfSigned(): Pair<java.security.KeyPair, X509Certificate> {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keyPair = kpg.generateKeyPair()

        val now = Date()
        val notAfter = Date(now.time + 1000L * 60 * 60 * 24 * 365 * 10)
        val serial = BigInteger(64, SecureRandom())
        val name = X500Principal("CN=TvRemote")

        val builder = JcaX509v3CertificateBuilder(
            name, serial, now, notAfter, name, keyPair.public
        )
        val signer = JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.private)
        val holder = builder.build(signer)
        val cert = JcaX509CertificateConverter().getCertificate(holder)
        return Pair(keyPair, cert)
    }
}
