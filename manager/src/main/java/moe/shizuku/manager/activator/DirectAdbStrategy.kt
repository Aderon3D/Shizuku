package moe.shizuku.manager.activator

import android.content.Context
import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Strategy 1: Direct ADB enable via WRITE_SECURE_SETTINGS.
 *
 * On stock Android 11+ (AOSP/Pixel/Motorola/Nokia), simply setting
 * `adb_wifi_enabled = 1` in Settings.Global triggers AdbService to
 * start adbd listening on a random TCP port in range 37000-44000.
 *
 * No Wi-Fi, no hotspot needed on these devices.
 *
 * On some OEMs (Samsung, Xiaomi, OnePlus), this may fail because
 * the system checks for an active network interface. In that case,
 * LocalHotspotStrategy should be used instead.
 */
class DirectAdbStrategy(private val context: Context) : ActivatorStrategy {

    companion object {
        private const val TAG = "DirectAdbStrategy"
        private const val POLL_ATTEMPTS = 10
        private const val POLL_DELAY_MS = 1500L
    }

    override val name: String get() = "direct_adb"

    override val description: String get() = "Enable wireless ADB via system settings"

    override suspend fun activate(log: ((String) -> Unit)?): Int {
        val cr = context.contentResolver

        if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            log?.invoke("WRITE_SECURE_SETTINGS not granted, skipping direct strategy")
            return -1
        }

        withContext(Dispatchers.IO) {
            log?.invoke("Enabling USB debugging...")
            Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)

            log?.invoke("Setting adb_allowed_connection_time...")
            Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)

            log?.invoke("Enabling wireless debugging (adb_wifi_enabled)...")
            Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
        }

        log?.invoke("Waiting for ADB daemon to start on TCP...")

        // Poll for ADB port
        for (i in 1..POLL_ATTEMPTS) {
            delay(POLL_DELAY_MS)

            val port = AdbPortScanner.scan()
            if (port > 0) {
                log?.invoke("ADB daemon detected on port $port")
                return port
            }

            if (i % 3 == 0) {
                log?.invoke("Still waiting for ADB... (attempt ${i}/$POLL_ATTEMPTS)")
            }
        }

        log?.invoke("ADB daemon did not start on TCP within timeout")
        return -1
    }

    /**
     * Check if this strategy has a chance of working on this device.
     */
    override suspend fun isAvailable(): Boolean {
        return context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }
}
