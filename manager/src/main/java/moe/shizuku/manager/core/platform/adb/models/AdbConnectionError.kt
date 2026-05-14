package moe.shizuku.manager.core.platform.adb.models

import java.io.IOException

sealed interface AdbConnectionError {
    data object NotPaired : AdbConnectionError
    data class ConnectionFailed(val e: IOException) : AdbConnectionError
}
