package com.onican.tvremote.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.onican.tvremote.proto.RemoteConfigure
import com.onican.tvremote.proto.RemoteDeviceInfo
import com.onican.tvremote.proto.RemoteDirection
import com.onican.tvremote.proto.RemoteKeyCode
import com.onican.tvremote.proto.RemoteKeyInject
import com.onican.tvremote.proto.RemoteMessage
import com.onican.tvremote.proto.RemotePingResponse
import javax.net.ssl.SSLSocket

interface TvRemoteListener {
    fun onReady() {}
    fun onDisconnected(error: Exception?) {}
    fun onVolumeChanged(level: Int, max: Int, muted: Boolean) {}
    fun onCurrentApp(packageName: String) {}
}

/**
 * Maintains the persistent remote-control connection to port 6466. Must be
 * called with a client certificate that has already completed pairing
 * (see [TvPairingClient]) - the TV recognizes the same certificate and skips
 * pairing again.
 */
class TvRemoteClient(
    context: Context,
    private val host: String,
    private val listener: TvRemoteListener,
    private val port: Int = 6466
) {
    private val certManager = TvCertificateManager(context)
    private val trustManager = CapturingTrustManager()
    private var socket: SSLSocket? = null
    private var running = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val writeLock = Any()

    fun connect() {
        Thread {
            try {
                val s = TvSslContext.openSocket(host, port, certManager.keyStore, "tvremote".toCharArray(), trustManager)
                socket = s
                running = true

                sendConfigure()
                mainHandler.post { listener.onReady() }
                readLoop(s)
            } catch (e: Exception) {
                mainHandler.post { listener.onDisconnected(e) }
            }
        }.start()
    }

    fun disconnect() {
        running = false
        try {
            socket?.close()
        } catch (_: Exception) {
        }
    }

    fun sendKey(keyCode: RemoteKeyCode, direction: RemoteDirection = RemoteDirection.SHORT) {
        val msg = RemoteMessage.newBuilder()
            .setRemoteKeyInject(
                RemoteKeyInject.newBuilder()
                    .setKeyCode(keyCode)
                    .setDirection(direction)
                    .build()
            )
            .build()
        sendSafely(msg)
    }

    private fun sendConfigure() {
        val msg = RemoteMessage.newBuilder()
            .setRemoteConfigure(
                RemoteConfigure.newBuilder()
                    .setCode1(622)
                    .setDeviceInfo(
                        RemoteDeviceInfo.newBuilder()
                            .setModel("TV Remote")
                            .setVendor("Onican")
                            .setUnknown1(1)
                            .setUnknown2("1")
                            .setPackageName("com.onican.tvremote")
                            .setAppVersion("1.0.0")
                            .build()
                    )
                    .build()
            )
            .build()
        sendSafely(msg)
    }

    private fun readLoop(s: SSLSocket) {
        while (running) {
            val bytes = MessageFraming.readFrame(s.inputStream) ?: break
            val msg = RemoteMessage.parseFrom(bytes)

            if (msg.hasRemotePingRequest()) {
                val pong = RemoteMessage.newBuilder()
                    .setRemotePingResponse(
                        RemotePingResponse.newBuilder().setVal1(msg.remotePingRequest.val1).build()
                    )
                    .build()
                sendSafely(pong)
            }
            if (msg.hasRemoteSetVolumeLevel()) {
                val v = msg.remoteSetVolumeLevel
                mainHandler.post { listener.onVolumeChanged(v.volumeLevel, v.volumeMax, v.volumeMuted) }
            }
        }
        running = false
        mainHandler.post { listener.onDisconnected(null) }
    }

    private fun sendSafely(message: RemoteMessage) {
        val s = socket ?: return
        Thread {
            try {
                synchronized(writeLock) {
                    MessageFraming.write(s.outputStream, message)
                }
            } catch (_: Exception) {
            }
        }.start()
    }
}
