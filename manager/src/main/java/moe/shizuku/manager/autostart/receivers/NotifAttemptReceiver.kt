package moe.shizuku.manager.autostart.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import moe.shizuku.manager.privilegedservice.PrivilegedServiceManager
import moe.shizuku.manager.autostart.AutoStartWorker
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class NotifAttemptReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        val privilegedServiceManager: PrivilegedServiceManager = get()
        AutoStartWorker.enqueue(context, privilegedServiceManager.isWifiRequired)
    }
}
