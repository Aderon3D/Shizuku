package moe.shizuku.manager.core.platform.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri
import moe.shizuku.manager.core.extensions.isTelevision

class BatteryOptimizationHelper(private val context: Context) {
    private val powerManager: PowerManager by systemService(context)

    val isIgnoringBatteryOptimizations: Boolean
        get() = context.isTelevision ||
                powerManager.isIgnoringBatteryOptimizations(context.packageName)

    val intent: Intent by lazy {
        @SuppressLint("BatteryLife")
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
        }
    }
}