package moe.shizuku.manager.intents

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import moe.shizuku.manager.R
import moe.shizuku.manager.core.platform.services.notifications.NotificationChannelProvider
import moe.shizuku.manager.core.platform.services.notifications.NotificationHelper
import moe.shizuku.manager.intents.models.TokenValidationError

class AuthErrorNotificationProvider(
    private val context: Context,
    private val notificationHelper: NotificationHelper
) : NotificationChannelProvider {

    companion object {
        const val CHANNEL_ID = "auth_errors"
        const val NOTIFICATION_ID = 1450
    }

    override fun provideChannel(): NotificationChannelCompat =
        NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.intents_auth_errors))
            .build()

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
            .Builder(context, CHANNEL_ID)
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
}
