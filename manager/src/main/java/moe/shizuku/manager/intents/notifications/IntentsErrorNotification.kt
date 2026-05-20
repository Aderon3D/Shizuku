package moe.shizuku.manager.intents.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import moe.shizuku.manager.R
import moe.shizuku.manager.core.platform.services.notifications.AppNotificationChannel
import moe.shizuku.manager.core.platform.services.notifications.NotificationHelper
import moe.shizuku.manager.intents.models.TokenValidationError

class IntentsErrorNotification(
    private val context: Context,
    private val channel: AppNotificationChannel,
    private val notificationHelper: NotificationHelper
) {

    fun showAuthErrorNotification(e: TokenValidationError) {
        val title = when (e) {
            TokenValidationError.TokenRequired -> R.string.intents_token_required
            TokenValidationError.TokenInvalid -> R.string.intents_token_invalid
        }

        val msg = when (e) {
            TokenValidationError.TokenRequired -> R.string.intents_token_required_message
            TokenValidationError.TokenInvalid -> R.string.intents_token_invalid_message
        }

        val notification = NotificationCompat
            .Builder(context, channel.id)
            .setContentTitle(context.getString(title))
            .setContentText(context.getString(msg))
            .setContentIntent(intentsPendingIntent)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_system_icon)
            .build()

        notificationHelper.notify(NOTIFICATION_ID, notification)
    }

    private val intentsPendingIntent by lazy {
        NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.intents_fragment)
            .createPendingIntent()
    }

    companion object {
        const val NOTIFICATION_ID = 1450
    }

}