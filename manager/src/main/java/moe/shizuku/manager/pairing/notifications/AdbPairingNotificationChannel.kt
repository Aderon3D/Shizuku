package moe.shizuku.manager.pairing.notifications

import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import moe.shizuku.manager.R
import moe.shizuku.manager.core.platform.services.notifications.AppNotificationChannel

data class AdbPairingNotificationChannel(
    override val id: String = "adb_pairing",
    override val name: Int = R.string.pairing_notification_channel,
    override val importance: Int = NotificationManagerCompat.IMPORTANCE_HIGH
) : AppNotificationChannel {
    override val customizer: (NotificationChannelCompat.Builder.() -> Unit) = {
        setShowBadge(false)
    }
}
