package moe.shizuku.manager.core.platform.adb.models

sealed interface AdbPortError {
    data object NotFound : AdbPortError
    data class SettingsError(val e: AdbSettingsError) : AdbPortError
}
