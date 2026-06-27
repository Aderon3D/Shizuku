package moe.shizuku.manager.activator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import moe.shizuku.manager.R
import moe.shizuku.manager.utils.SettingsPage
import kotlin.coroutines.resume

/**
 * Strategy 3: Guided fallback — shows a notification asking the user
 * to manually enable hotspot or connect to Wi-Fi.
 *
 * This is the last resort when automatic strategies fail. It creates
 * a persistent notification that guides the user to enable a network
 * connection (either hotspot or Wi-Fi), then sets adb_wifi_enabled
 * and waits for ADB to appear.
 *
 * The user just needs to tap the Quick Settings hotspot tile once.
 */
class GuidedFallbackStrategy(private val context: Context) : ActivatorStrategy {

    companion object {
        private const val TAG = "GuidedFallbackStrategy"
        private const val CHANNEL_ID = "activator_guided"
        private const val NOTIFICATION_ID = 1449
        private const val MAX_WAIT_MS = 120_000L
        private const val POLL_INTERVAL_MS = 2000L

        const val ACTION_HOTSPOT_DONE = "moe.shizuku.manager.activator.HOTSPOT_DONE"
        const val ACTION_CANCEL = "moe.shizuku.manager.activator.CANCEL"
    }

    override val name: String get() = "guided_fallback"

    override val description: String get() = "Guide user to enable hotspot or Wi-Fi"

    override suspend fun activate(log: ((String) -> Unit)?): Int {
        log?.invoke("Showing guided activation notification...")

        return withContext(Dispatchers.IO) {
            showGuidanceNotification()

            // Wait for the user to enable hotspot/Wi-Fi
            val port = waitForAdb(log)

            // Clean up notification
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)

            if (port > 0) {
                log?.invoke("ADB detected on port $port after user action")
            } else {
                log?.invoke("Timed out waiting for user action")
            }

            port
        }
    }

    private fun showGuidanceNotification() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Guided ADB Activation",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications to guide ADB activation setup"
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)

        val tapDoneIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(ACTION_HOTSPOT_DONE).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = PendingIntent.getBroadcast(
            context, 1,
            Intent(ACTION_CANCEL).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val wifiIntent = SettingsPage.InternetPanel.buildIntent(context)
        val wifiPendingIntent = PendingIntent.getActivity(
            context, 2, wifiIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentTitle("Enable Hotspot or Wi-Fi")
            .setContentText("Enable hotspot or connect to Wi-Fi to start Shizuku, then tap Done")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(android.R.drawable.ic_menu_share, "Done", tapDoneIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .setContentIntent(wifiPendingIntent)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    private suspend fun waitForAdb(log: ((String) -> Unit)?): Int {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var cancelled = false
        var userDone = false

        // Register broadcast receivers for notification actions
        val doneReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                userDone = true
                // User says they've enabled hotspot/Wi-Fi, try setting adb_wifi_enabled
                Settings.Global.putInt(ctx.contentResolver, Settings.Global.ADB_ENABLED, 1)
                Settings.Global.putLong(ctx.contentResolver, "adb_allowed_connection_time", 0L)
                Settings.Global.putInt(ctx.contentResolver, "adb_wifi_enabled", 1)

                // Update notification
                val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_share)
                    .setContentTitle("Activating Shizuku...")
                    .setContentText("Detecting ADB service...")
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build()
                nm.notify(NOTIFICATION_ID, notification)
            }
        }

        val cancelReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                cancelled = true
            }
        }

        return try {
            context.registerReceiver(doneReceiver, IntentFilter(ACTION_HOTSPOT_DONE),
                Context.RECEIVER_NOT_EXPORTED)
            context.registerReceiver(cancelReceiver, IntentFilter(ACTION_CANCEL),
                Context.RECEIVER_NOT_EXPORTED)

            val deadline = System.currentTimeMillis() + MAX_WAIT_MS
            var port = -1

            while (System.currentTimeMillis() < deadline && !cancelled) {
                // If the user tapped Done or we haven't checked yet, try to detect ADB
                port = AdbPortScanner.scan()
                if (port > 0) return port

                delay(POLL_INTERVAL_MS)
            }

            if (cancelled) {
                log?.invoke("User cancelled guided activation")
            }

            -1
        } finally {
            try { context.unregisterReceiver(doneReceiver) } catch (_: Exception) {}
            try { context.unregisterReceiver(cancelReceiver) } catch (_: Exception) {}
        }
    }
}
