package moe.shizuku.manager.permission.data

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import moe.shizuku.manager.core.extensions.getAppLabel
import moe.shizuku.manager.core.extensions.toUserId
import moe.shizuku.manager.core.platform.services.user.DeviceUserRepository
import moe.shizuku.manager.core.platform.services.pkg.PackageInfoRepository
import moe.shizuku.manager.permission.PermissionManager
import moe.shizuku.manager.permission.models.AuthorizedAppsItem

class AuthorizedAppsRepository(
    private val context: Context,
    private val deviceUserRepository: DeviceUserRepository,
    private val packageInfoRepository: PackageInfoRepository,
    private val permissionManager: PermissionManager
) {
    // StateFlow so we can cache the app list across screens, as it's an expensive operation
    private val _appsList = MutableStateFlow<List<AuthorizedAppsItem.App>?>(null)
    val appsList: SharedFlow<List<AuthorizedAppsItem.App>?> =
        _appsList.asStateFlow().onSubscription {
            if (_appsList.value == null) {
                getAuthorizedApps()
            }
        }

    val grantedCount: Flow<Int> = _appsList.asStateFlow().filterNotNull().map { list ->
        list.count { it.isGranted }
    }.distinctUntilChanged()

    suspend fun getAuthorizedApps(allUsers: Boolean = true) = withContext(Dispatchers.IO) {
        val users = if (allUsers) {
            deviceUserRepository.getUsers()
        } else {
            listOf(deviceUserRepository.getCurrentUser())
        }

        val appsWithPermission = users.flatMap { user ->
            packageInfoRepository.getInstalledPackages(
                PackageManager.GET_PERMISSIONS,
                user.id
            )
        }.filter { app ->
            app.packageName != context.packageName &&
                    app.applicationInfo != null &&
                    permissionManager.isPermissionDeclared(app)
        }

        _appsList.value = appsWithPermission.map { pkgInfo ->
            async {
                val appInfo = pkgInfo.applicationInfo ?: return@async null

                AuthorizedAppsItem.App(
                    appInfo = appInfo,
                    isGranted = runCatching {
                        permissionManager.isGranted(appInfo.uid)
                    }.getOrDefault(false),
                    user = deviceUserRepository.getUser(appInfo.uid.toUserId),
                    label = context.getAppLabel(appInfo)
                )
            }
        }.awaitAll()
            .filterNotNull()
            .sortedBy { it.displayName }
    }

    fun updatePermission(app: AuthorizedAppsItem.App, granted: Boolean): Result<Unit> =
        runCatching {
            permissionManager.setGranted(app.uid, granted)
        }.onSuccess {
            _appsList.update { currentList ->
                currentList?.map {
                    if (it == app) {
                        it.copy(isGranted = granted)
                    } else it
                }
            }
        }

}