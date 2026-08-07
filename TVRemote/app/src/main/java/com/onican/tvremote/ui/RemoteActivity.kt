package com.onican.tvremote.ui

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.onican.tvremote.R
import com.onican.tvremote.network.TvRemoteClient
import com.onican.tvremote.network.TvRemoteListener
import com.onican.tvremote.proto.RemoteKeyCode

class RemoteActivity : AppCompatActivity(), TvRemoteListener {

    private lateinit var client: TvRemoteClient
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remote)

        val ip = intent.getStringExtra("ip") ?: run { finish(); return }
        tvStatus = findViewById(R.id.tvConnStatus)

        client = TvRemoteClient(this, ip, this)
        client.connect()

        bindKey(R.id.btnUp, RemoteKeyCode.KEYCODE_DPAD_UP)
        bindKey(R.id.btnDown, RemoteKeyCode.KEYCODE_DPAD_DOWN)
        bindKey(R.id.btnLeft, RemoteKeyCode.KEYCODE_DPAD_LEFT)
        bindKey(R.id.btnRight, RemoteKeyCode.KEYCODE_DPAD_RIGHT)
        bindKey(R.id.btnOk, RemoteKeyCode.KEYCODE_DPAD_CENTER)
        bindKey(R.id.btnBack, RemoteKeyCode.KEYCODE_BACK)
        bindKey(R.id.btnHome, RemoteKeyCode.KEYCODE_HOME)
        bindKey(R.id.btnPower, RemoteKeyCode.KEYCODE_POWER)
        bindKey(R.id.btnVolUp, RemoteKeyCode.KEYCODE_VOLUME_UP)
        bindKey(R.id.btnVolDown, RemoteKeyCode.KEYCODE_VOLUME_DOWN)
        bindKey(R.id.btnMute, RemoteKeyCode.KEYCODE_MUTE)
        bindKey(R.id.btnPlayPause, RemoteKeyCode.KEYCODE_MEDIA_PLAY_PAUSE)
        bindKey(R.id.btnRewind, RemoteKeyCode.KEYCODE_MEDIA_REWIND)
        bindKey(R.id.btnForward, RemoteKeyCode.KEYCODE_MEDIA_FAST_FORWARD)
        bindKey(R.id.btnSource, RemoteKeyCode.KEYCODE_TV_INPUT)
        bindKey(R.id.btnSettings, RemoteKeyCode.KEYCODE_SETTINGS)
    }

    private fun bindKey(viewId: Int, keyCode: RemoteKeyCode) {
        val view = findViewById<android.view.View>(viewId)
        view.setOnClickListener { client.sendKey(keyCode) }
    }

    override fun onReady() {
        tvStatus.text = "Terhubung"
    }

    override fun onDisconnected(error: Exception?) {
        tvStatus.text = if (error != null) "Terputus: ${error.message}" else "Terputus"
    }

    override fun onVolumeChanged(level: Int, max: Int, muted: Boolean) {
        // Optional: reflect volume state in the UI.
    }

    override fun onDestroy() {
        super.onDestroy()
        client.disconnect()
    }
}
