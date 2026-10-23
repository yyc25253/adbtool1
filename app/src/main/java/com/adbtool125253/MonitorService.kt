package com.adbtool125253

import android.app.*
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.core.app.NotificationCompat

class MonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var observer: ContentObserver
    private var wakeLock: PowerManager.WakeLock? = null

    private val watchedUris = listOf(
        Settings.Global.getUriFor(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED),
        Settings.Global.getUriFor(Settings.Global.ADB_ENABLED),
        Settings.Global.getUriFor("adb_wifi_enabled"),
    )

    private data class State(val dev: Int, val adb: Int, val wifi: Int)
    private var lastState: State? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification())

        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Monitor::PollLock")
        wakeLock?.acquire()

        observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) = checkAndAct()
        }
        watchedUris.forEach { contentResolver.registerContentObserver(it, false, observer) }

        handler.post(pollRunnable)
        checkAndAct()
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            checkAndAct()
            handler.postDelayed(this, 3000)
        }
    }

    private fun checkAndAct() {
        val now = State(
            Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0),
            Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0),
            Settings.Global.getInt(contentResolver, "adb_wifi_enabled", 0),
        )
        val prev = lastState
        lastState = now

        if (prev == now) return

        if (prev != null) {
            if (prev.dev == 1 && now.dev == 0) onDeveloperOptionsOff()
            if (prev.adb == 1 && now.adb == 0) onAdbOff()
            if (prev.wifi == 1 && now.wifi == 0) onWifiAdbOff()
        }

        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, buildNotification())
    }

    private fun onDeveloperOptionsOff() {
        try {
            Settings.Global.putInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1)
        } catch (e: SecurityException) {
            android.util.Log.w("Monitor", "写回开发者选项失败: ${e.message}")
        }
    }

    private fun onAdbOff() {
        try {
            Settings.Global.putInt(contentResolver, Settings.Global.ADB_ENABLED, 1)
        } catch (e: SecurityException) {
            android.util.Log.w("Monitor", "写回 ADB 失败: ${e.message}")
        }
    }

    private fun onWifiAdbOff() {
        try {
            Settings.Global.putInt(contentResolver, "adb_wifi_enabled", 1)
        } catch (e: SecurityException) {
            android.util.Log.w("Monitor", "写回无线调试失败: ${e.message}")
        }
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(
            CHANNEL_ID, "开关监控", NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
        }
        nm.createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val s = lastState
        val text = if (s == null) "初始化中"
        else "开发者:${s.dev} ADB:${s.adb} 无线:${s.wifi}"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("开关监控运行中")
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(observer)
        handler.removeCallbacks(pollRunnable)
        wakeLock?.release()
        wakeLock = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "monitor_v2"
        private const val NOTIF_ID = 1001
    }
}
