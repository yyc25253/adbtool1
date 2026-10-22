package com.adbtool125253

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var btn: ImageButton
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("monitor", MODE_PRIVATE)
        btn = findViewById(R.id.toggleBtn)
        status = findViewById(R.id.statusText)

        btn.setOnClickListener { toggle() }
        render()

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun toggle() {
        val enabled = !prefs.getBoolean("enabled", false)
        prefs.edit().putBoolean("enabled", enabled).apply()

        if (enabled) {
            startForegroundService(Intent(this, MonitorService::class.java))
        } else {
            stopService(Intent(this, MonitorService::class.java))
        }
        render()
    }

    private fun render() {
        val on = prefs.getBoolean("enabled", false)
        btn.isSelected = on
        status.text = if (on) "运行中" else "已停止"
        status.setTextColor(if (on) 0xFF1ED760.toInt() else 0xFF888888.toInt())
    }
}
