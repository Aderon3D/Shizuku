package moe.shizuku.manager.privilegedservice.models

sealed interface PrivilegedServiceError {
    data object NotRunning : PrivilegedServiceError
}
