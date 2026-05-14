package moe.shizuku.manager.autostart

import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.github.michaelbull.result.fold
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import moe.shizuku.manager.autostart.AutoStartNotificationProvider.Companion.RUNNING_ID
import moe.shizuku.manager.autostart.models.AutoStartState
import moe.shizuku.manager.core.extensions.isWifiConnected
import moe.shizuku.manager.core.platform.device.AndroidVersion
import moe.shizuku.manager.privilegedservice.PrivilegedServiceManager
import moe.shizuku.manager.start.StartStep

class AutoStartWorker(
    private val context: Context,
    params: WorkerParameters,
    private val notificationProvider: AutoStartNotificationProvider,
    private val privilegedServiceManager: PrivilegedServiceManager
) : CoroutineWorker(context, params) {

    private var isSuccess = false

    override suspend fun doWork(): Result = try {
        notificationProvider.updateNotification(AutoStartState.Running(isAwaitingAuth = false))

        coroutineScope {
            val session = privilegedServiceManager.createStartSession()

            val job = launch {
                session.steps.collect { steps ->
                    checkAwaitingAuth(steps)
                }
            }

            privilegedServiceManager.startService(session).also {
                job.cancel()
            }
        }.fold(
            success = {
                isSuccess = true
                Result.success()
            },
            failure = {
                notificationProvider.showErrorNotification()
                Result.retry()
            }
        )
    } finally {
        val state = resolveAutoStartState(isSuccess)
        notificationProvider.updateNotification(state)
    }

    private fun resolveAutoStartState(isSuccess: Boolean): AutoStartState =
        if (!isStopped) {
            if (isSuccess) AutoStartState.Success
            else AutoStartState.Waiting.Retry
        } else if (AndroidVersion.isAtLeast12) {
            when (stopReason) {
                WorkInfo.STOP_REASON_CANCELLED_BY_APP -> AutoStartState.Cancelled
                WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY -> AutoStartState.Waiting.Wifi
                else -> AutoStartState.Waiting.Retry
            }
        } else {
            if (!context.isWifiConnected) AutoStartState.Waiting.Wifi
            else AutoStartState.Waiting.Retry
        }

    private var isForeground = false

    private suspend fun checkAwaitingAuth(steps: List<StartStep<*, *>>) {
        val enableWirelessDebuggingStep =
            steps.filterIsInstance<StartStep.EnableWirelessDebugging>().firstOrNull()

        val isAwaitingAuth = enableWirelessDebuggingStep?.isAwaitingAuth ?: false

        if (isAwaitingAuth && !isForeground) {
            setForeground(getForegroundInfo())
            isForeground = true
        }

        notificationProvider.updateNotification(AutoStartState.Running(isAwaitingAuth))
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val fgsType = if (AndroidVersion.isAtLeast14) {
            FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0

        return ForegroundInfo(
            RUNNING_ID,
            notificationProvider.buildNotification(
                AutoStartState.Running(isAwaitingAuth = true)
            ),
            fgsType
        )
    }

    companion object {
        const val WORK_NAME = "adb_start_worker"
    }
}
