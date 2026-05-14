package moe.shizuku.manager.core.platform.adb.models

sealed interface AdbSettingsError {
    data object NoWriteSecureSettings : AdbSettingsError
    data object NotSupported : AdbSettingsError
    data object NoWifi : AdbSettingsError
    data object NotAuthorized : AdbSettingsError
}
