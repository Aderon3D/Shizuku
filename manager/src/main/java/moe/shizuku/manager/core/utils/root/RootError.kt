package moe.shizuku.manager.core.utils.root

sealed interface RootError {
    data object PermissionDenied : RootError
}