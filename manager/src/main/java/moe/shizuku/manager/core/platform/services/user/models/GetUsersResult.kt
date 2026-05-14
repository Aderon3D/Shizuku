package moe.shizuku.manager.core.platform.services.user.models

sealed interface GetUsersError {
    data object PrivilegedServiceNotRunning : GetUsersError
    data object NoPermission : GetUsersError
    data object NotFound : GetUsersError
}
