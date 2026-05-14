package moe.shizuku.manager.autostart.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import moe.shizuku.manager.autostart.AutoStartManager
import moe.shizuku.manager.autostart.AutoStartWorker
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class NotifCancelReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        get<AutoStartManager>().cancel()
    }
}
