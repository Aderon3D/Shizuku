package moe.shizuku.manager.privilegedservice.models

import moe.shizuku.manager.core.utils.Version

sealed class PrivilegedServiceState {

    data class Running(
        val uid: Int,
        val version: Version
    ) : PrivilegedServiceState()
    object Starting : PrivilegedServiceState()
    object Stopping : PrivilegedServiceState()
    object Stopped : PrivilegedServiceState()
    object Crashed : PrivilegedServiceState()
}
