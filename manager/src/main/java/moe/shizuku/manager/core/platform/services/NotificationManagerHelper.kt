package moe.shizuku.manager.core.platform.services

import android.app.NotificationManager
import android.content.Context
import android.os.Build

class NotificationManagerHelper(context: Context) {
    private val notificationManager: NotificationManager by systemService(context)

    fun isNotificationChannelEnabled(channelId: String): Boolean {
        if (!notificationManager.areNotificationsEnabled()) return false

        // Notification channels don't exist on Android 7, so notification is enabled
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true

        // Channel hasn't been created yet, so it will be enabled once created
        val channel = notificationManager.getNotificationChannel(channelId)
            ?: return true

        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }
}