package moe.shizuku.manager.home.models

sealed class PrivilegedServiceUiState {
    data class Running(
        val mode: Mode,
        val version: String,
        val isLatestVersion: Boolean,
        val canGrantPermission: Boolean
    ) : PrivilegedServiceUiState() {
        enum class Mode {
            ROOT, ADB
        }
    }
    object Stopped : PrivilegedServiceUiState()
}
