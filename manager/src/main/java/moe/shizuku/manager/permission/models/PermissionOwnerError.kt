package moe.shizuku.manager.permission.models

sealed interface PermissionOwnerError {
    data object PermissionNotFound : PermissionOwnerError
}
