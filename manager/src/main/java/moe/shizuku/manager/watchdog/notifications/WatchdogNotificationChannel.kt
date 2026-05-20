package moe.shizuku.manager.watchdog.notifications

import androidx.core.app.NotificationManagerCompat
import moe.shizuku.manager.R
import moe.shizuku.manager.core.platform.services.notifications.AppNotificationChannel

data class WatchdogNotificationChannel(
    override val id: String = "shizuku_watchdog",
    override val name: Int = R.string.settings_watchdog,
    override val importance: Int = NotificationManagerCompat.IMPORTANCE_LOW
) : AppNotificationChannel
