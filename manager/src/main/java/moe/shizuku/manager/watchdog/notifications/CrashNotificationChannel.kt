package moe.shizuku.manager.watchdog.notifications

import moe.shizuku.manager.R
import moe.shizuku.manager.core.platform.services.notifications.AppNotificationChannel

data class CrashNotificationChannel(
    override val id: String = "crash_reports",
    override val name: Int = R.string.watchdog_crash_reports
) : AppNotificationChannel
