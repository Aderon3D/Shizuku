package moe.shizuku.manager.home.models

import moe.shizuku.manager.tcpmode.models.TcpState
import moe.shizuku.manager.watchdog.models.WatchdogState

data class HomeUiState(
    val serviceState: PrivilegedServiceUiState,
    val watchdogState: WatchdogState,
    val tcpState: TcpState,
    val isStealthModeActive: Boolean,
    val authorizedAppsCount: Int
)
