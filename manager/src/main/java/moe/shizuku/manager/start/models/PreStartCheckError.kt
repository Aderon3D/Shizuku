package moe.shizuku.manager.start.models

sealed interface PreStartCheckError {
    sealed interface Background : PreStartCheckError

    sealed interface Foreground : PreStartCheckError

    data object NotRooted : PreStartCheckError, Background, Foreground
    data object TlsNotSupported : PreStartCheckError, Background, Foreground
    data object WriteSecureSettingsNotGranted : PreStartCheckError, Background
    data object UsbDebuggingDisabled : PreStartCheckError, Foreground
    data object WirelessDebuggingDisabled : PreStartCheckError, Foreground
    data object WifiRequired : PreStartCheckError, Foreground
    data object AuthorizationRequired : PreStartCheckError, Foreground
}