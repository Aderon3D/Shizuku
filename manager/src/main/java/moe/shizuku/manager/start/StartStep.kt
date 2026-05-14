package moe.shizuku.manager.start

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.onOk
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import moe.shizuku.manager.core.platform.adb.AdbPortHelper
import moe.shizuku.manager.core.platform.adb.AdbSession
import moe.shizuku.manager.core.platform.adb.AdbSettingsManager
import moe.shizuku.manager.core.platform.adb.client.AdbClient
import moe.shizuku.manager.core.platform.adb.models.AdbConnectionError
import moe.shizuku.manager.core.platform.adb.models.AdbPortError
import moe.shizuku.manager.core.platform.adb.models.AdbSettingsError
import moe.shizuku.manager.core.preferences.models.StartMode
import moe.shizuku.manager.core.utils.root.RootError
import moe.shizuku.manager.core.utils.root.RootUtils
import moe.shizuku.manager.core.utils.runnable.Runnable
import moe.shizuku.manager.privilegedservice.PrivilegedServiceStateMachine
import moe.shizuku.manager.tcpmode.TcpManager

sealed class StartStep<out T, out E> : Runnable<T, E>() {

    class RequestRootPermission : StartStep<Unit, RootError>() {

        override suspend fun onRun() =
            RootUtils.requestRootPermission()
    }

    class EnableUsbDebugging(
        private val adbSettingsManager: AdbSettingsManager
    ) : StartStep<Unit, AdbSettingsError>() {

        override suspend fun onRun() =
            adbSettingsManager.setUsbDebugging(true)
    }

    class EnableWirelessDebugging(
        private val adbSettingsManager: AdbSettingsManager
    ) : StartStep<Unit, AdbSettingsError>() {

        var isAwaitingAuth: Boolean = false
            private set

        @RequiresApi(Build.VERSION_CODES.R)
        override suspend fun onRun() =
            adbSettingsManager.setWirelessDebugging(true) { isAwaitingAuth ->
                this@EnableWirelessDebugging.isAwaitingAuth = isAwaitingAuth
                refresh()
            }

    }

    // TODO errors
    class CloseTcpPort(
        private val adbSession: AdbSession,
        private val tcpManager: TcpManager
    ) : StartStep<Unit, Unit>() {

        override suspend fun onRun() = try {
            tcpManager.closeTcpPort(adbSession)
            Ok(Unit)
        } catch (_: Exception) {
            Err(Unit)
        }

    }

    class SearchForPort(
        private val adbSession: AdbSession,
        private val adbPortHelper: AdbPortHelper,
        private val isWifiRequired: Boolean
    ) : StartStep<Int, AdbPortError>() {

        override suspend fun onRun() =
            adbPortHelper.getAdbPort(forceTls = isWifiRequired)
                .onOk { adbSession.port = it }

    }

    class ConnectToPort(
        private val adbSession: AdbSession
    ) : StartStep<AdbClient, AdbConnectionError>() {

        override suspend fun onRun() =
            adbSession.connect()
    }

    // TODO errors
    class OpenTcpPort(
        private val adbSession: AdbSession,
        private val tcpManager: TcpManager,
        private val targetPort: Int
    ) : StartStep<Unit, String?>() {

        override suspend fun onRun() = try {
            tcpManager.openTcpPort(targetPort, adbSession)
            Ok(Unit)
        } catch (e: Exception) {
            Err(e.message)
        }

    }

    // TODO errors
    class ExecuteCommand(
        private val adbSession: AdbSession?,
        private val startMode: StartMode,
        private val internalCommand: String
    ) : StartStep<Unit, List<String>>() {

        override suspend fun onRun() = when (startMode) {
            StartMode.ROOT -> {
                withContext(Dispatchers.IO) {
                    val result = Shell.cmd(internalCommand).exec()
                    if (result.isSuccess) Ok(Unit)
                    else Err(result.err)
                }
            }

            StartMode.WADB -> {
                adbSession!!.withClient { client ->
                    client.command("shell:$internalCommand") { }
                }.fold(
                    success = { Ok(Unit) },
                    failure = { error ->
                        val message = when (error) {
                            is AdbConnectionError.ConnectionFailed -> error.e.stackTraceToString()
                            else -> error.toString()
                        }
                        Err(listOf(message))
                    }
                )
            }
        }
    }

    class WaitForService(
        private val privilegedServiceStateMachine: PrivilegedServiceStateMachine
    ) : StartStep<Unit, Unit>() {

        override suspend fun onRun() =
            withTimeoutOrNull(60_000L) {
                privilegedServiceStateMachine.isRunningFlow.first { true }
                Ok(Unit)
            } ?: Err(Unit)

    }
}