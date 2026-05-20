package moe.shizuku.manager.intents.notifications

import moe.shizuku.manager.R
import moe.shizuku.manager.core.platform.services.notifications.AppNotificationChannel

data class IntentsNotificationChannel(
    override val id: String = "auth_errors",
    override val name: Int = R.string.intents_auth_errors
) : AppNotificationChannel
