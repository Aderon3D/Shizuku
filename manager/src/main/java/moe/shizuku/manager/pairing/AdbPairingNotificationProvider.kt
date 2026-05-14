package moe.shizuku.manager.pairing

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Action.SEMANTIC_ACTION_REPLY
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.app.RemoteInput
import androidx.navigation.NavDeepLinkBuilder
import com.google.android.material.color.MaterialColors
import moe.shizuku.manager.R
import moe.shizuku.manager.core.platform.services.notifications.NotificationChannelProvider
import moe.shizuku.manager.core.platform.services.notifications.NotificationHelper
import moe.shizuku.manager.pairing.models.PairingState
import moe.shizuku.manager.pairing.services.AdbPairingService

@RequiresApi(Build.VERSION_CODES.R)
class AdbPairingNotificationProvider(
    private val context: Context,
    private val notificationHelper: NotificationHelper
) : NotificationChannelProvider {

    override fun provideChannel(): NotificationChannelCompat =
        NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.pairing_notification_channel))
            .setSound(null, null)
            .setShowBadge(false)
            .build()

    fun updateNotification(state: PairingState) {
        val notification = buildNotification(state)
        notificationHelper.notify(NOTIFICATION_ID, notification)
    }

    fun buildNotification(state: PairingState): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setColor(MaterialColors.getColor(context, android.R.attr.colorPrimary, 0))
            .setSmallIcon(R.drawable.ic_system_icon)

        when (state) {
            is PairingState.Searching -> {
                builder.setContentTitle(context.getString(R.string.pairing_searching))
                    .setOngoing(true)
                    .addAction(stopNotificationAction)
            }
            is PairingState.Ready -> {
                builder.setContentTitle(context.getString(R.string.pairing_service_found))
                    .setOngoing(true)
                    .addAction(replyNotificationAction)
            }
            is PairingState.Working -> {
                builder.setContentTitle(context.getString(R.string.pairing_in_progress))
                    .setOngoing(true)
            }
            is PairingState.Success -> {
                builder.setContentTitle(context.getString(R.string.pairing_successful))
                    .setContentText(context.getString(R.string.pairing_successful_message))
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setContentIntent(homePendingIntent)
                    .addAction(startNotificationAction)
            }
            is PairingState.Failure -> {
                builder.setContentTitle(context.getString(R.string.pairing_failed))
                    .setContentText(state.message)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .addAction(retryNotificationAction)
            }
        }
        return builder.build()
    }

    private val homePendingIntent by lazy {
        NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.home_fragment)
            .createPendingIntent()
    }

    private val startNotificationAction by lazy {
        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.start_fragment)
            .createPendingIntent()

        NotificationCompat.Action.Builder(
            null,
            context.getString(R.string.start),
            pendingIntent
        ).build()
    }

    private val stopNotificationAction by lazy {
        NotificationCompat.Action.Builder(
            null,
            context.getString(R.string.pairing_stop_searching),
            getServicePendingIntent(AdbPairingService.Action.STOP, RequestCode.STOP, false)
        ).build()
    }

    private val retryNotificationAction by lazy {
        NotificationCompat.Action.Builder(
            null,
            context.getString(R.string.retry),
            getServicePendingIntent(AdbPairingService.Action.START, RequestCode.RETRY, false)
        ).build()
    }

    private val replyNotificationAction by lazy {
        val remoteInput = RemoteInput.Builder(PAIRING_CODE_KEY)
            .setLabel(context.getString(R.string.pairing_enter_code))
            .build()

        NotificationCompat.Action.Builder(
            null,
            context.getString(R.string.pairing_enter_code),
            getServicePendingIntent(AdbPairingService.Action.REPLY, RequestCode.REPLY, true)
        )
            .addRemoteInput(remoteInput)
            .setSemanticAction(SEMANTIC_ACTION_REPLY) // TODO check if works
            .build()
    }

    private fun getServicePendingIntent(
        action: AdbPairingService.Action,
        requestCode: RequestCode,
        isMutable: Boolean
    ): PendingIntent {
        val intent = Intent(context, AdbPairingService::class.java)
            .setAction(action.name)

        return PendingIntentCompat.getForegroundService(
            context,
            requestCode.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT,
            isMutable
        )
    }

    private enum class RequestCode {
        REPLY, STOP, RETRY
    }

    companion object {
        const val CHANNEL_ID = "adb_pairing"
        const val NOTIFICATION_ID = 1

        const val PAIRING_CODE_KEY = "pairing_code"
    }

}