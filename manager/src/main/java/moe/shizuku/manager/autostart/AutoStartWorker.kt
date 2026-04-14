package moe.shizuku.manager.autostart

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.platform.device.AndroidVersion
import moe.shizuku.manager.core.utils.runnable.RunnableStatus
import moe.shizuku.manager.privilegedservice.PrivilegedServiceManager
import moe.shizuku.manager.privilegedservice.data.ShizukuStateMachine
import moe.shizuku.manager.privilegedservice.models.StartStep
import java.io.EOFException
import java.util.concurrent.TimeoutException

class AutoStartWorker(
    context: Context,
    params: WorkerParameters,
    private val autoStartManager: AutoStartManager,
    private val shizukuStateMachine: ShizukuStateMachine,
    private val privilegedServiceManager: PrivilegedServiceManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            autoStartManager.updateNotification(
                AutoStartManager.WorkerState.RUNNING,
            )

            val session = privilegedServiceManager.createStartSession()

            coroutineScope {
                val job = launch {
                    session.steps.collect { steps ->
                        val wadbStep = steps.find { it is StartStep.EnableWirelessDebugging }
                            as? StartStep.EnableWirelessDebugging
                        if (wadbStep?.isAwaitingAuth == true) {
                            Log.i("AutoStartWorker", "Awaiting authorization, promoting to FGS")

                            autoStartManager.updateNotification(
                                AutoStartManager.WorkerState.AWAITING_AUTHORIZATION
                            )

                            val fgsType = if (AndroidVersion.isAtLeast14) {
                                FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                            } else 0

                            runCatching {
                                val foregroundInfo = ForegroundInfo(
                                    10,
                                    autoStartManager.fgsNotif(),
                                    fgsType
                                )

                                setForeground(foregroundInfo)
                            }.onFailure {
                                Log.e(TAG, "Failed to promote to FGS", it)
                            }
                        } else if (wadbStep?.status == RunnableStatus.Completed) {
                            autoStartManager.updateNotification(
                                AutoStartManager.WorkerState.RUNNING
                            )
                        }
                    }
                }

                privilegedServiceManager.startService(session)
                job.cancel()
            }

            val nm =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(AutoStartManager.NOTIFICATION_ID)

            return Result.success()
        } catch (e: CancellationException) {
            val state = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                AutoStartManager.WorkerState.AWAITING_RETRY
            } else {
                when (stopReason) {
                    WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY -> AutoStartManager.WorkerState.AWAITING_WIFI
                    WorkInfo.STOP_REASON_CANCELLED_BY_APP -> AutoStartManager.WorkerState.STOPPED
                    else -> AutoStartManager.WorkerState.AWAITING_RETRY
                }
            }
            autoStartManager.updateNotification(state)

            throw e
        } catch (e: Exception) {
            val ignored = listOf(
                EOFException::class,
                SecurityException::class,
                TimeoutException::class,
            )
            if (ignored.none { it.isInstance(e) }) showErrorNotification(applicationContext, e)

            if (shizukuStateMachine.update() == ShizukuStateMachine.State.RUNNING) {
                return Result.success()
            } else {
                autoStartManager.updateNotification(
                    AutoStartManager.WorkerState.AWAITING_RETRY
                )
                return Result.retry()
            }
        }
    }

    private fun showErrorNotification(
        context: Context,
        e: Exception,
    ) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.start_background),
            NotificationManager.IMPORTANCE_LOW,
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)

        val nb = NotificationCompat.Builder(context, CHANNEL_ID)

        val notification = nb.setSmallIcon(R.drawable.ic_system_icon)
            .setContentTitle(context.getString(R.string.start_background_error))
            .setContentText(e.message).setSilent(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(e.message)).build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        fun enqueue(context: Context, isWifiRequired: Boolean) {
            val cb = Constraints.Builder()
            if (isWifiRequired) {
                cb.setRequiredNetworkType(NetworkType.UNMETERED)
            }
            val constraints = cb.build()

            val request =
                OneTimeWorkRequestBuilder<AutoStartWorker>().setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "adb_start_worker",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        const val CHANNEL_ID: String = "AdbStartWorker"
        const val NOTIFICATION_ID: Int = 1448
    }
}