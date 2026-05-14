package moe.shizuku.manager.intents.receivers

import android.content.Context
import android.content.Intent
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.privilegedservice.PrivilegedServiceManager
import org.koin.core.component.get

class ManualStopReceiver : AuthenticatedReceiver() {
    override fun onAuthenticated(context: Context, intent: Intent) {
        if (intent.action != ACTION) return

        get<PrivilegedServiceManager>().stopService()
    }

    companion object {
        const val ACTION = "${BuildConfig.APPLICATION_ID}.STOP"
    }
}