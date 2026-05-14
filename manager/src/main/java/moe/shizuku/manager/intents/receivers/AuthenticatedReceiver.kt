package moe.shizuku.manager.intents.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import moe.shizuku.manager.intents.AuthErrorNotificationProvider
import moe.shizuku.manager.intents.usecases.ValidateTokenUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

abstract class AuthenticatedReceiver : BroadcastReceiver(), KoinComponent {

    final override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val validateTokenUseCase: ValidateTokenUseCase = get()
        val notificationProvider: AuthErrorNotificationProvider = get()

        val authToken = intent.getStringExtra("auth")

        validateTokenUseCase(authToken)
            .onOk { onAuthenticated(context, intent) }
            .onErr { notificationProvider.showAuthErrorNotification(it) }
    }

    abstract fun onAuthenticated(
        context: Context,
        intent: Intent
    )
}