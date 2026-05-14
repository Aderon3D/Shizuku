package moe.shizuku.manager.watchdog.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import moe.shizuku.manager.R
import moe.shizuku.manager.core.platform.services.notifications.NotificationChannelProvider
import moe.shizuku.manager.core.platform.services.notifications.NotificationHelper
import moe.shizuku.manager.watchdog.services.WatchdogService

class WatchdogNotificationProvider(
    private val context: Context,
    private val notificationHelper: NotificationHelper
) : NotificationChannelProvider {

    companion object {
        const val ID_WATCHDOG: Int = 1001
        const val CHANNEL_ID_WATCHDOG: String = "shizuku_watchdog"
    }

    override fun provideChannel(): NotificationChannelCompat =
        NotificationChannelCompat.Builder(CHANNEL_ID_WATCHDOG, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(context.getString(R.string.settings_watchdog))
            .build()

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

        return NotificationCompat.Builder(context, CHANNEL_ID_WATCHDOG)
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
}
