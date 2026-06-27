package moe.shizuku.manager.activator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import moe.shizuku.manager.R
import moe.shizuku.manager.MainActivity
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbStarter
import moe.shizuku.manager.receiver.ShizukuReceiverStarter
import moe.shizuku.manager.service.WatchdogService
import moe.shizuku.manager.starter.Starter
import moe.shizuku.manager.utils.ShizukuStateMachine
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground service that runs the full Shizuku activation chain.
 *
 * Flow:
 * 1. Run ActivatorEngine (try strategies in order)
 * 2. Once ADB port is found, call AdbStarter.startAdb() to:
 *    a. Switch to TCP mode (adb tcpip 5555) if enabled
 *    b. Execute the Shizuku start command
 * 3. Wait for Shizuku binder
 * 4. Disable wireless debugging (adb_wifi_enabled = 0)
 *    since TCP mode makes it unnecessary
 * 5. Start WatchdogService if enabled
 * 6. Stop self
 */
class ActivatorService : Service() {

    companion object {
        private const val TAG = "ActivatorService"
        private const val CHANNEL_ID = "shizuku_activator"
        private const val NOTIFICATION_ID = 1450

        private val isRunning = AtomicBoolean(false)
        private var serviceJob: Job? = null
        private var scope: CoroutineScope? = null

        @JvmStatic
        fun start(context: Context) {
            try {
                val intent = Intent(context, ActivatorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start ActivatorService", e)
            }
        }

        @JvmStatic
        fun stop(context: Context) {
            context.stopService(Intent(context, ActivatorService::class.java))
        }

        @JvmStatic
        fun isRunning(): Boolean = isRunning.get()
    }

    private val activationLog = mutableListOf<String>()

    override fun onCreate() {
        super.onCreate()
        isRunning.set(true)
        Log.d(TAG, "ActivatorService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_STOP") {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Starting Shizuku..."))

        val jobScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope = jobScope
        serviceJob = jobScope.launch {
            runActivation()
        }

        return START_STICKY
    }

    private suspend fun runActivation() {
        try {
            updateNotification("Activating ADB...")

            val engine = ActivatorEngine(this)
            val result = engine.activate { msg ->
                Log.d(TAG, msg)
                synchronized(activationLog) { activationLog.add(msg) }
                updateNotification(msg.take(60))
            }

            if (!result.success) {
                updateNotification("ADB activation failed")
                Log.w(TAG, "Activation failed: ${result.error}")
                delay(5000)
                stopSelf()
                return
            }

            // ADB is now listening on result.port
            updateNotification("Starting Shizuku service...")

            // Ensure USB debugging stays on during startup
            if (ShizukuSettings.getTcpMode()) {
                Log.d(TAG, "TCP mode enabled, will switch to port ${ShizukuSettings.getTcpPort()}")
            }

            // AdbStarter.startAdb handles:
            // - TCP mode switch (adb tcpip <port>) if enabled
            // - Executing Shizuku start command
            // - Disabling adb_wifi_enabled when done
            AdbStarter.startAdb(this, result.port) { msg ->
                synchronized(activationLog) { activationLog.add(msg) }
            }

            updateNotification("Waiting for Shizuku binder...")

            // Wait for Shizuku to connect its binder
            Starter.waitForBinder()

            updateNotification("Shizuku is running!")
            Log.i(TAG, "Shizuku started successfully via ${result.strategyName}")

            // Start watchdog if enabled
            if (ShizukuSettings.getWatchdog()) {
                WatchdogService.start(this)
            }

            delay(2000) // Let the user see success
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Activation failed with exception", e)
            val errorMsg = "${e::class.simpleName}: ${e.message}"
            synchronized(activationLog) { activationLog.add(errorMsg) }
            updateNotification("Error: ${e.message?.take(50) ?: "unknown"}")

            // Check if Shizuku is running despite the error
            if (ShizukuStateMachine.isRunning()) {
                Log.i(TAG, "Shizuku is running despite activation error")
                if (ShizukuSettings.getWatchdog()) {
                    WatchdogService.start(this)
                }
            } else {
                ShizukuReceiverStarter.updateNotification(this, ShizukuReceiverStarter.WorkerState.AWAITING_RETRY)
            }

            delay(5000)
            stopSelf()
        }
    }

    override fun onDestroy() {
        isRunning.set(false)
        serviceJob?.cancel()
        scope?.cancel()

        // Clean up hotspot if it was started
        try {
            LocalHotspotStrategy.hotspotReservation?.close()
            LocalHotspotStrategy.hotspotReservation = null
        } catch (_: Exception) {}

        // Cancel notification
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)

        super.onDestroy()
        Log.d(TAG, "ActivatorService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(text: String): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Shizuku Activator",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows Shizuku activation progress"
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Shizuku Activator")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            nm.notify(NOTIFICATION_ID, buildNotification(text))
        } catch (_: Exception) {}
    }
}
