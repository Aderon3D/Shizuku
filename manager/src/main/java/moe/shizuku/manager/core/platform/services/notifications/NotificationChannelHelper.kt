package moe.shizuku.manager.core.platform.services.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat

class NotificationChannelHelper(
    private val context: Context,
    private val notificationChannels: List<NotificationChannelProvider>
) {
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    fun createChannels() {
        val channels = notificationChannels.map { it.provideChannel() }
        notificationManager.createNotificationChannelsCompat(channels)
    }
}