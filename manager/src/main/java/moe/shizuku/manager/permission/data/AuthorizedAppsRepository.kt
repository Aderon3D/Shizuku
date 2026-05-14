package moe.shizuku.manager.permission.data

import android.content.Context
import android.content.pm.PackageManager
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import moe.shizuku.manager.core.extensions.getAppLabel
import moe.shizuku.manager.core.extensions.toUserId
import moe.shizuku.manager.core.platform.services.packages.manager.PackageInfoRepository
import moe.shizuku.manager.core.platform.services.user.DeviceUserRepository
import moe.shizuku.manager.permission.PermissionManager
import moe.shizuku.manager.permission.models.AuthorizedAppsItem
import moe.shizuku.manager.permission.models.PermissionToggleError

class AuthorizedAppsRepository(
    private val context: Context,
    private val deviceUserRepository: DeviceUserRepository,
    private val packageInfoRepository: PackageInfoRepository,
    private val permissionManager: PermissionManager
) {
    private val _appsList = MutableStateFlow<List<AuthorizedAppsItem.App>?>(null)
    val appsList: StateFlow<List<AuthorizedAppsItem.App>?> = _appsList.asStateFlow()

    val grantedCount: Flow<Int> = _appsList.asStateFlow().filterNotNull().map { list ->
        list.count { it.isGranted }
    }.distinctUntilChanged()

    suspend fun getAuthorizedApps(allUsers: Boolean = true) {
        val users = if (allUsers) {
            deviceUserRepository.getUsers().fold(
                success = { it },
                failure = { emptyList() }
            )
        } else {
            listOf(deviceUserRepository.getCurrentUser())
        }

        val appsWithPermission = users.flatMap { user ->
            packageInfoRepository.getInstalledPackages(
                PackageManager.GET_PERMISSIONS,
                user.id
            ).fold(
                success = { it },
                failure = { emptyList() }
            )
        }.filter { app ->
            val isManagerApp = app.packageName == context.packageName

            !isManagerApp &&
                    app.applicationInfo != null &&
                    permissionManager.isPermissionDeclared(app)
        }

        _appsList.value = appsWithPermission.map { pkgInfo ->
            withContext(Dispatchers.Default) {
                async {
                    val appInfo = pkgInfo.applicationInfo ?: return@async null

                    AuthorizedAppsItem.App(
                        appInfo = appInfo,
                        isGranted = permissionManager.isGranted(appInfo.uid).getOr(false),
                        user = deviceUserRepository.getUser(appInfo.uid.toUserId),
                        label = context.getAppLabel(appInfo)
                    )
                }
            }
        }.awaitAll()
            .filterNotNull()
            .sortedBy { it.displayName }
    }

    suspend fun updatePermission(
        app: AuthorizedAppsItem.App,
        granted: Boolean
    ): Result<Unit, PermissionToggleError> =
        permissionManager.setGranted(app.uid, granted).onOk {
            _appsList.update { currentList ->
                currentList?.map {
                    if (it == app) it.copy(isGranted = granted)
                    else it
                }
            }
        }

}
