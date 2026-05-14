package moe.shizuku.manager.tcpmode

import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.platform.adb.AdbPortHelper
import moe.shizuku.manager.core.platform.adb.AdbSession
import moe.shizuku.manager.core.platform.adb.AdbSettingsManager
import moe.shizuku.manager.tcpmode.models.TcpState
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.get
import java.io.EOFException
import java.io.IOException
import java.net.SocketException

class TcpManager(
    private val adbSettingsManager: AdbSettingsManager,
    private val adbPortHelper: AdbPortHelper,
    private val adbSessionFactory: AdbSession.Factory,
    preferencesRepository: PreferencesRepository
) {

    // TCP STATE

    private val refreshSignal = Channel<Unit>(Channel.CONFLATED)
    private val refreshFlow = refreshSignal.receiveAsFlow()

    // TCP PORT CHECKS

    private val tcpPort: Int?
        get() = adbPortHelper.tcpPort.get()

    val isTcpPortOpen: Boolean
        get() = tcpPort != null

    fun isTcpPortOpen(targetPort: Int): Boolean =
        tcpPort == targetPort

    val tcpState: Flow<TcpState> = combine(
        preferencesRepository.tcpMode.flow,
        preferencesRepository.tcpPort.flow,
        refreshFlow
    ) { tcpMode, targetPort, _ ->
        TcpState(
            current =
                tcpPort?.let { TcpState.Port.Enabled(it) } ?:
                TcpState.Port.Disabled
            ,
            target =
                if (tcpMode) TcpState.Port.Enabled(targetPort)
                else TcpState.Port.Disabled
        )
    }

    fun refresh() {
        refreshSignal.trySend(Unit)
    }

    // OPEN/CLOSE TCP PORT

    suspend fun openTcpPort(targetPort: Int) {
        if (isTcpPortOpen(targetPort)) return

        adbSessionFactory.create(tcpPort ?: return).use { session ->
            openTcpPort(targetPort, session)
        }
    }

    suspend fun openTcpPort(targetPort: Int, session: AdbSession): Result<Unit> =
        if (isTcpPortOpen(targetPort)) Result.success(Unit)
        else session.withClient { client ->
            runCatching {
                client.command("tcpip:$targetPort")
            }.onFailure {
                if (it !is EOFException && it !is SocketException) throw it
            }
        }.fold(
            success = {
                session.port = targetPort
                refresh()
                Result.success(Unit)
            },
            failure = { error ->
                val exception = when (error) {
                    is moe.shizuku.manager.core.platform.adb.models.AdbConnectionError.ConnectionFailed -> error.e
                    else -> IOException(error.toString())
                }
                Log.e(TAG, "Couldn't open TCP port", exception)
                refresh()
                Result.failure(exception)
            }
        )

    suspend fun closeTcpPort() {

        adbSessionFactory.create(tcpPort ?: return).use { session ->
            closeTcpPort(session)
        }
    }

    suspend fun closeTcpPort(session: AdbSession): Result<Unit> =
        if (!isTcpPortOpen) Result.success(Unit)
        else {
            check(adbSettingsManager.setUsbDebugging(true).isOk)
            { "USB debugging not enabled" }

            session.withClient { client ->
                client.command("usb:")
            }.fold(
                success = {
                    session.port = 0
                    refresh()
                    Result.success(Unit)
                },
                failure = { error ->
                    val exception = when (error) {
                        is moe.shizuku.manager.core.platform.adb.models.AdbConnectionError.ConnectionFailed -> error.e
                        else -> IOException(error.toString())
                    }
                    Log.e(TAG, "Couldn't close TCP port", exception)
                    refresh()
                    Result.failure(exception)
                }
            )
        }
}