package moe.shizuku.manager.core.platform.services.packages.manager

sealed interface GetInstalledPackagesError {
    data object NoPermission : GetInstalledPackagesError
    data object PrivilegedServiceNotRunning : GetInstalledPackagesError
}
