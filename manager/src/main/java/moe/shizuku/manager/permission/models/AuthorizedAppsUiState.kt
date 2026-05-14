package moe.shizuku.manager.permission.models

sealed class AuthorizedAppsUiState {
    object Loading : AuthorizedAppsUiState()

    data class Error(
        val error: AuthorizedAppsError
    ) : AuthorizedAppsUiState()

    data class Success(
        val apps: List<AuthorizedAppsItem.App>,
        val isRefreshing: Boolean = false,
        val isServiceRunning: Boolean
    ) : AuthorizedAppsUiState() {
        val isAppListEmpty: Boolean get() = apps.isEmpty()
        val areAllAppsGranted: Boolean get() = apps.all { it.isGranted }
    }
}
