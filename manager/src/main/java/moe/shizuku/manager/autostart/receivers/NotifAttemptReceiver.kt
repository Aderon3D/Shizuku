package moe.shizuku.manager.autostart.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import moe.shizuku.manager.autostart.AutoStartManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class NotifAttemptReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        get<AutoStartManager>().start()
    }
}
