package moe.shizuku.manager.permission.models

interface PermissionToggleError {
    data object PrivilegedServiceNotRunning : PermissionToggleError
    data object AdbRestricted : PermissionToggleError
    data object DeviceUserAccessRestricted : PermissionToggleError
    data object SecurityError : PermissionToggleError
}