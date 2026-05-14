package moe.shizuku.manager.watchdog.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import moe.shizuku.manager.R
import moe.shizuku.manager.core.platform.services.notifications.NotificationChannelProvider
import moe.shizuku.manager.core.platform.services.notifications.NotificationHelper
import moe.shizuku.manager.core.platform.settings.SystemSettingsPage

class CrashNotificationProvider(
    private val context: Context,
    private val notificationHelper: NotificationHelper
) : NotificationChannelProvider {

    companion object {
        const val ID_CRASH: Int = 1002
        const val CHANNEL_ID_CRASH: String = "crash_reports"
    }

    override fun provideChannel(): NotificationChannelCompat =
        NotificationChannelCompat.Builder(CHANNEL_ID_CRASH, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.watchdog_crash_reports))
            .build()

    fun showCrashNotification() {
        val learnMoreIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "https://github.com/thedjchi/Shizuku/wiki/troubleshooting#shizuku-keeps-stopping-randomly".toUri()
        }
        val learnMorePendingIntent = PendingIntent.getActivity(
            context, 0, learnMoreIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val disableIntent = SystemSettingsPage.Notifications.NotificationChannel(CHANNEL_ID_CRASH)
            .buildIntent(context)
        val disablePendingIntent = PendingIntent.getActivity(
            context, 0, disableIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_CRASH)
            .setContentTitle(context.getString(R.string.watchdog_crash_alert))
            .setContentText(context.getString(R.string.watchdog_crash_alert_message))
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentIntent(learnMorePendingIntent)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.watchdog_disable_alerts), disablePendingIntent)
            .build()

        notificationHelper.notify(ID_CRASH, notification)
    }
}
