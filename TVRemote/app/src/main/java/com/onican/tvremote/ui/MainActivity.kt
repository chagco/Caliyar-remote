package com.onican.tvremote.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.onican.tvremote.R

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("tvremote", MODE_PRIVATE)

        val editIp = findViewById<EditText>(R.id.editIp)
        editIp.setText(prefs.getString("last_ip", ""))

        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            val ip = editIp.text.toString().trim()
            if (ip.isEmpty()) {
                Toast.makeText(this, "Masukkan alamat IP TV dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit().putString("last_ip", ip).apply()

            val paired = prefs.getStringSet("paired_ips", emptySet())?.contains(ip) == true
            if (paired) {
                startActivity(Intent(this, RemoteActivity::class.java).putExtra("ip", ip))
            } else {
                startActivity(Intent(this, PairingActivity::class.java).putExtra("ip", ip))
            }
        }

        findViewById<Button>(R.id.btnPair).setOnClickListener {
            val ip = editIp.text.toString().trim()
            if (ip.isEmpty()) {
                Toast.makeText(this, "Masukkan alamat IP TV dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit().putString("last_ip", ip).apply()
            startActivity(Intent(this, PairingActivity::class.java).putExtra("ip", ip))
        }
    }
}
