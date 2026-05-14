package moe.shizuku.manager.permission.models

interface AuthorizedAppsError {
    data object PrivilegedServiceNotRunning : AuthorizedAppsError
    data object NoPermission : AuthorizedAppsError
}