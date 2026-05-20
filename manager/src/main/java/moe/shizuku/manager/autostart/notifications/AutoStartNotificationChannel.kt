package moe.shizuku.manager.autostart.notifications

import androidx.core.app.NotificationManagerCompat
import moe.shizuku.manager.R
import moe.shizuku.manager.core.platform.services.notifications.AppNotificationChannel

data class AutoStartNotificationChannel(
    override val id: String = "autostart",
    override val name: Int = R.string.start_channel,
    override val importance: Int = NotificationManagerCompat.IMPORTANCE_LOW
) : AppNotificationChannel
