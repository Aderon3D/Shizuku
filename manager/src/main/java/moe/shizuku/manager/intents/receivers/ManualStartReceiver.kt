package moe.shizuku.manager.intents.receivers

import android.content.Context
import android.content.Intent
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.autostart.AutoStartManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class ManualStartReceiver : AuthenticatedReceiver(), KoinComponent {
    override fun onAuthenticated(context: Context, intent: Intent) {
        if (intent.action != ACTION) return

        get<AutoStartManager>().start()
    }

    companion object {
        const val ACTION = "${BuildConfig.APPLICATION_ID}.START"
    }
}
