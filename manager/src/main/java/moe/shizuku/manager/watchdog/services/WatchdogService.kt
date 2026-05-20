package moe.shizuku.manager.watchdog.services

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.github.michaelbull.result.onErr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import moe.shizuku.manager.autostart.AutoStartManager
import moe.shizuku.manager.core.platform.device.AndroidVersion
import moe.shizuku.manager.core.extensions.resultOf
import moe.shizuku.manager.privilegedservice.PrivilegedServiceStateMachine
import moe.shizuku.manager.privilegedservice.models.PrivilegedServiceState
import moe.shizuku.manager.watchdog.notifications.CrashNotification
import moe.shizuku.manager.watchdog.notifications.WatchdogNotification
import org.koin.android.ext.android.inject

class WatchdogService : Service() {

    private val notificationProvider: WatchdogNotification by inject()
    private val crashNotificationProvider: CrashNotification by inject()
    private val autoStartManager: AutoStartManager by inject()
    private val privilegedServiceStateMachine: PrivilegedServiceStateMachine by inject()
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate() {
        super.onCreate()
        _isRunning.value = true

        privilegedServiceStateMachine.state
            .onEach { state ->
                if (state is PrivilegedServiceState.Crashed) {
                    crashNotificationProvider.showCrashNotification()
                    autoStartManager.start()
                }
            }
            .launchIn(scope)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        val fgsType = if (AndroidVersion.isAtLeast14) {
            FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0

        resultOf {
            ServiceCompat.startForeground(
                this,
                WatchdogNotification.ID_WATCHDOG,
                notificationProvider.createWatchdogNotification(),
                fgsType
            )
        }.onErr {
            val isFgsRestriction = AndroidVersion.isAtLeast12 &&
                    it is ForegroundServiceStartNotAllowedException
            if (!isFgsRestriction) stopSelf() else throw it
        }
        return START_STICKY
    }

    override fun onDestroy() {
        _isRunning.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP_SERVICE: String = "ACTION_STOP_SERVICE"
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    }
}
