package moe.shizuku.manager.core.platform.services.notifications

import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

interface AppNotificationChannel {
    val id: String
    val name: Int
    val importance: Int
        get() = NotificationManagerCompat.IMPORTANCE_DEFAULT

    val customizer: (NotificationChannelCompat.Builder.() -> Unit)
        get() = {}
}