package moe.shizuku.manager.pairing.services

import android.app.RemoteInput
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ServiceCompat
import com.github.michaelbull.result.onErr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.toast
import moe.shizuku.manager.core.platform.adb.client.AdbKey
import moe.shizuku.manager.core.platform.adb.AdbMdns
import moe.shizuku.manager.core.platform.adb.client.AdbPairingClient
import moe.shizuku.manager.core.platform.adb.client.PairingCodeException
import moe.shizuku.manager.core.platform.adb.client.PreferenceAdbKeyStore
import moe.shizuku.manager.core.platform.device.AndroidVersion
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import com.github.michaelbull.result.onOk
import moe.shizuku.manager.core.extensions.resultOf
import moe.shizuku.manager.pairing.AdbPairingNotificationProvider
import moe.shizuku.manager.pairing.models.PairingState
import org.koin.android.ext.android.inject
import java.net.ConnectException

@RequiresApi(Build.VERSION_CODES.R)
class AdbPairingService : Service() {
    private val preferencesRepository: PreferencesRepository by inject()
    private val adbMdns: AdbMdns by inject()
    private val notificationProvider: AdbPairingNotificationProvider by inject()

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var discoveryJob: Job? = null
    private var pairingJob: Job? = null
    private var timeoutJob: Job? = null

    private val _state = MutableStateFlow<PairingState>(PairingState.Searching)
    private val state = _state.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            state.collect { state ->
                notificationProvider.updateNotification(state)

                when (state) {
                    is PairingState.Searching -> {
                        startDiscovery()
                    }
                    is PairingState.Working -> {
                        discoveryJob?.cancel()
                        doPairing(state.port, state.code)
                    }
                    else -> {
                        discoveryJob?.cancel()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action?.let { name ->
            Action.entries.find { it.name == name }
                ?: throw IllegalArgumentException(
                    "Unknown action: $name. Intent must use one of AdbPairingService.Action names."
                )
        }

        when (action) {
            Action.START -> startService()
            Action.STOP -> stopService()
            Action.REPLY -> handleReply(intent)
            null -> Unit
        }

        return START_NOT_STICKY
    }

    private fun startService() {
        val notification = notificationProvider.buildNotification(state.value)

        val foregroundServiceType = if (AndroidVersion.isAtLeast14) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
        } else 0

        ServiceCompat.startForeground(
            this,
            AdbPairingNotificationProvider.NOTIFICATION_ID,
            notification,
            foregroundServiceType
        )

        scheduleTimeout()
    }

    private fun scheduleTimeout() {
        timeoutJob?.cancel()
        timeoutJob = serviceScope.launch {
            delay(TIMEOUT_MS)
            toast(R.string.pairing_timed_out)
            stopService()
        }
    }

    private fun startDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = serviceScope.launch {
            adbMdns.pairingFlow.collectLatest { port ->
                Log.i(TAG, "Pairing service port: $port")
                if (port > 0) {
                    _state.value = PairingState.Ready(port)
                } else {
                    _state.value = PairingState.Searching
                }
            }
        }
    }

    private fun handleReply(intent: Intent) {
        val code = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(AdbPairingNotificationProvider.PAIRING_CODE_KEY)?.toString()

        if (code != null) {
            val currentState = state.value
            if (currentState is PairingState.Ready) {
                _state.value = PairingState.Working(currentState.port, code)
            } else {
                startService()
            }
        } else {
            startService()
        }
    }

    private fun doPairing(port: Int, code: String) {
        pairingJob?.cancel()
        pairingJob = serviceScope.launch(Dispatchers.IO) {
            resultOf {
                val key = AdbKey(PreferenceAdbKeyStore(preferencesRepository.prefs), "shizuku")
                AdbPairingClient("127.0.0.1", port, code, key).start()
            }
                .onOk {
                    _state.value = PairingState.Success
                }
                .onErr {
                    val msg = when (it) {
                        is ConnectException -> getString(R.string.start_error_connection)
                        is PairingCodeException -> getString(R.string.pairing_error_invalid_code)
                        else -> it.localizedMessage ?: getString(R.string.pairing_failed)
                    }
                    _state.value = PairingState.Failure(msg)
                }

            withContext(Dispatchers.Main) {
                stopService()
            }
        }
    }

    override fun onTimeout(startId: Int) {
        toast(R.string.pairing_timed_out)
        stopService()
    }

    private fun stopService() {
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    enum class Action {
        START, STOP, REPLY
    }

    companion object {
        private const val TIMEOUT_MS = 180_000L // Same as short FGS

        fun startIntent(context: Context): Intent =
            Intent(context, AdbPairingService::class.java)
                .setAction(Action.START.name)
    }

}
