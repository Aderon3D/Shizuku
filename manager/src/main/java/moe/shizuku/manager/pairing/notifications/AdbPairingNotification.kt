package moe.shizuku.manager.pairing.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.app.RemoteInput
import androidx.navigation.NavDeepLinkBuilder
import com.google.android.material.color.MaterialColors
import moe.shizuku.manager.core.platform.services.notifications.AppNotificationChannel
import moe.shizuku.manager.core.platform.services.notifications.NotificationHelper
import moe.shizuku.manager.pairing.models.PairingState
import moe.shizuku.manager.pairing.services.AdbPairingService

@RequiresApi(Build.VERSION_CODES.R)
class AdbPairingNotification(
    private val context: Context,
    private val channel: AppNotificationChannel,
    private val notificationHelper: NotificationHelper
) {

    fun updateNotification(state: PairingState) {
        val notification = buildNotification(state)
        notificationHelper.notify(NOTIFICATION_ID, notification)
    }

    fun buildNotification(state: PairingState): Notification {
        val builder = NotificationCompat.Builder(context, channel.id)
            .setColor(MaterialColors.getColor(context, android.R.attr.colorPrimary, 0))
            .setSmallIcon(moe.shizuku.manager.R.drawable.ic_system_icon)

        when (state) {
            is PairingState.Searching -> {
                builder.setContentTitle(context.getString(moe.shizuku.manager.R.string.pairing_searching))
                    .setOngoing(true)
                    .addAction(stopNotificationAction)
            }
            is PairingState.Ready -> {
                builder.setContentTitle(context.getString(moe.shizuku.manager.R.string.pairing_service_found))
                    .setOngoing(true)
                    .addAction(replyNotificationAction)
            }
            is PairingState.Working -> {
                builder.setContentTitle(context.getString(moe.shizuku.manager.R.string.pairing_in_progress))
                    .setOngoing(true)
            }
            is PairingState.Success -> {
                builder.setContentTitle(context.getString(moe.shizuku.manager.R.string.pairing_successful))
                    .setContentText(context.getString(moe.shizuku.manager.R.string.pairing_successful_message))
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setContentIntent(homePendingIntent)
                    .addAction(startNotificationAction)
            }
            is PairingState.Failure -> {
                builder.setContentTitle(context.getString(moe.shizuku.manager.R.string.pairing_failed))
                    .setContentText(state.message)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .addAction(retryNotificationAction)
            }
        }
        return builder.build()
    }

    fun isChannelEnabled() =
        notificationHelper.isNotificationChannelEnabled(channel.id)

    private val homePendingIntent by lazy {
        NavDeepLinkBuilder(context)
            .setGraph(moe.shizuku.manager.R.navigation.nav_graph)
            .setDestination(moe.shizuku.manager.R.id.home_fragment)
            .createPendingIntent()
    }

    private val startNotificationAction by lazy {
        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(moe.shizuku.manager.R.navigation.nav_graph)
            .setDestination(moe.shizuku.manager.R.id.start_fragment)
            .createPendingIntent()

        NotificationCompat.Action.Builder(
            null,
            context.getString(moe.shizuku.manager.R.string.start),
            pendingIntent
        ).build()
    }

    private val stopNotificationAction by lazy {
        NotificationCompat.Action.Builder(
            null,
            context.getString(moe.shizuku.manager.R.string.pairing_stop_searching),
            getServicePendingIntent(AdbPairingService.Action.STOP, RequestCode.STOP, false)
        ).build()
    }

    private val retryNotificationAction by lazy {
        NotificationCompat.Action.Builder(
            null,
            context.getString(moe.shizuku.manager.R.string.retry),
            getServicePendingIntent(AdbPairingService.Action.START, RequestCode.RETRY, false)
        ).build()
    }

    private val replyNotificationAction by lazy {
        val remoteInput = RemoteInput.Builder(PAIRING_CODE_KEY)
            .setLabel(context.getString(moe.shizuku.manager.R.string.pairing_enter_code))
            .build()

        NotificationCompat.Action.Builder(
            null,
            context.getString(moe.shizuku.manager.R.string.pairing_enter_code),
            getServicePendingIntent(AdbPairingService.Action.REPLY, RequestCode.REPLY, true)
        )
            .addRemoteInput(remoteInput)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY) // TODO check if works
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
        const val NOTIFICATION_ID = 1
        const val PAIRING_CODE_KEY = "pairing_code"
    }

}