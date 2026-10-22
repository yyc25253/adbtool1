package com.adbtool125253

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val on = context.getSharedPreferences("monitor", Context.MODE_PRIVATE)
            .getBoolean("enabled", false)
        if (!on) return

        try {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(Intent(context, MonitorService::class.java))
            } else {
                context.startService(Intent(context, MonitorService::class.java))
            }
        } catch (e: Exception) {
            // 部分 ROM 限制开机启动前台服务，失败就等用户下次点开
        }
    }
}
