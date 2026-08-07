package com.onican.tvremote.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.onican.tvremote.R
import com.onican.tvremote.network.TvPairingClient

class PairingActivity : AppCompatActivity() {

    private lateinit var ip: String
    private lateinit var client: TvPairingClient
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pairing)

        ip = intent.getStringExtra("ip") ?: run { finish(); return }
        client = TvPairingClient(this, ip)

        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val progress = findViewById<ProgressBar>(R.id.progress)
        val codeLayout = findViewById<View>(R.id.codeLayout)
        val editCode = findViewById<EditText>(R.id.editCode)

        Thread {
            try {
                client.startPairing()
                mainHandler.post {
                    progress.visibility = View.GONE
                    tvStatus.text = "Kode pairing sedang tampil di layar TV"
                    codeLayout.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                mainHandler.post {
                    tvStatus.text = "Gagal terhubung: ${e.message}"
                    progress.visibility = View.GONE
                }
            }
        }.start()

        findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            val code = editCode.text.toString().trim().uppercase()
            if (code.length != 6) {
                Toast.makeText(this, "Kode harus 6 digit", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            tvStatus.text = "Memverifikasi kode..."
            codeLayout.visibility = View.GONE
            progress.visibility = View.VISIBLE

            Thread {
                try {
                    val success = client.submitCode(code)
                    mainHandler.post {
                        if (success) {
                            savePaired(ip)
                            Toast.makeText(this, "Berhasil dipasangkan!", Toast.LENGTH_SHORT).show()
                            startActivity(
                                Intent(this, RemoteActivity::class.java)
                                    .putExtra("ip", ip)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            )
                            finish()
                        } else {
                            tvStatus.text = "Pairing gagal, coba lagi"
                            progress.visibility = View.GONE
                            codeLayout.visibility = View.VISIBLE
                        }
                    }
                } catch (e: Exception) {
                    mainHandler.post {
                        tvStatus.text = "Kode salah atau koneksi terputus: ${e.message}"
                        progress.visibility = View.GONE
                        codeLayout.visibility = View.VISIBLE
                    }
                }
            }.start()
        }
    }

    private fun savePaired(ip: String) {
        val prefs = getSharedPreferences("tvremote", MODE_PRIVATE)
        val current = HashSet(prefs.getStringSet("paired_ips", emptySet()) ?: emptySet())
        current.add(ip)
        prefs.edit().putStringSet("paired_ips", current).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        Thread { client.close() }.start()
    }
}
