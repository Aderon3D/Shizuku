package moe.shizuku.manager.watchdog.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import moe.shizuku.manager.R
import moe.shizuku.manager.core.platform.services.notifications.AppNotificationChannel
import moe.shizuku.manager.watchdog.services.WatchdogService

class WatchdogNotification(
    private val context: Context,
    private val channel: AppNotificationChannel
) {

    fun createWatchdogNotification(): Notification {
        val launchPendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.home_fragment)
            .createPendingIntent()

        val stopIntent = Intent(context, WatchdogService::class.java).apply {
            action = WatchdogService.ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            context, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, channel.id)
            .setContentTitle(context.getString(R.string.watchdog_running))
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentIntent(launchPendingIntent)
            .addAction(
                R.drawable.ic_close_24,
                context.getString(R.string.disable),
                stopPendingIntent,
            )
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        const val ID_WATCHDOG: Int = 1001
    }

}
