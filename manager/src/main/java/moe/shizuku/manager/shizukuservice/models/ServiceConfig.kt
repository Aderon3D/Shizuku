package moe.shizuku.manager.shizukuservice.models

import moe.shizuku.manager.core.data.preferences.StartMode

data class ServiceConfig(
    val startMode: StartMode,
    val startOnBoot: Boolean,
    val tcpMode: TcpMode,
    val autoDebuggingOff: Boolean
)

data class TcpMode(
    val enabled: Boolean,
    val port: Int
)