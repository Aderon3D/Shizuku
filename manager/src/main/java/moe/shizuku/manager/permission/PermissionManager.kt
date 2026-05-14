package moe.shizuku.manager.permission

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.mapError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shizuku.manager.core.extensions.isBinderDeadException
import moe.shizuku.manager.core.extensions.resultOf
import moe.shizuku.manager.core.extensions.toUserId
import moe.shizuku.manager.core.utils.resultOfPrivilegedService
import moe.shizuku.manager.permission.models.PermissionOwnerError
import moe.shizuku.manager.permission.models.PermissionToggleError
import moe.shizuku.manager.privilegedservice.ServiceMetadataRepository
import moe.shizuku.manager.privilegedservice.models.PrivilegedServiceError
import rikka.shizuku.Shizuku

class PermissionManager(
    private val context: Context,
    private val metadataRepository: ServiceMetadataRepository
) {
    suspend fun isGranted(uid: Int): Result<Boolean, PrivilegedServiceError> =
        withContext(Dispatchers.IO) {
            resultOfPrivilegedService {
                (Shizuku.getFlagsForUid(uid, MASK_PERMISSION) and FLAG_ALLOWED) == FLAG_ALLOWED
            }
        }


    suspend fun setGranted(uid: Int, grant: Boolean): Result<Unit, PermissionToggleError> {
        val result = if (grant) grant(uid) else revoke(uid)

        return result.mapError {
            when {
                it.isBinderDeadException -> PermissionToggleError.PrivilegedServiceNotRunning
                it is SecurityException -> {
                    val canGrantPermission = canGrantPermission()
                        .getOrElse { return@mapError PermissionToggleError.PrivilegedServiceNotRunning }
                    val serviceUid = metadataRepository.getUid()
                        .getOrElse { return@mapError PermissionToggleError.PrivilegedServiceNotRunning }

                    if (!canGrantPermission)
                        PermissionToggleError.AdbRestricted
                    else if (serviceUid.toUserId != uid.toUserId)
                        PermissionToggleError.DeviceUserAccessRestricted
                    else PermissionToggleError.SecurityError
                }

                else -> throw it
            }
        }
    }

    private suspend fun grant(uid: Int): Result<Unit, Exception> =
        withContext(Dispatchers.IO) {
            resultOf {
                Shizuku.updateFlagsForUid(uid, MASK_PERMISSION, FLAG_ALLOWED)
            }
        }

    private suspend fun revoke(uid: Int): Result<Unit, Exception> =
        withContext(Dispatchers.IO) {
            resultOf {
                Shizuku.updateFlagsForUid(uid, MASK_PERMISSION, 0)
            }
        }

    fun isPermissionOwner(): Result<Boolean, PermissionOwnerError> =
        resultOf {
            context.packageManager.getPermissionGroupInfo(PERMISSION_GROUP, 0)
            val info = context.packageManager.getPermissionInfo(PERMISSION, 0)

            info.packageName == context.packageName
        }.mapError {
            when (it) {
                is PackageManager.NameNotFoundException -> PermissionOwnerError.PermissionNotFound
                else -> throw it
            }
        }

    suspend fun canGrantPermission(): Result<Boolean, PrivilegedServiceError> =
        withContext(Dispatchers.IO) {
            resultOfPrivilegedService {
                Shizuku.checkRemotePermission(GRANT_RUNTIME_PERMISSIONS) == PackageManager.PERMISSION_GRANTED
            }
        }

    fun isPermissionDeclared(pkg: PackageInfo): Boolean =
        pkg.requestedPermissions?.contains(PERMISSION) ?: false

    companion object {
        private const val PERMISSION_GROUP = "moe.shizuku.manager.permission-group.API"
        private const val PERMISSION = "moe.shizuku.manager.permission.API_V23"
        private const val GRANT_RUNTIME_PERMISSIONS = "android.permission.GRANT_RUNTIME_PERMISSIONS"
        private const val FLAG_ALLOWED = 1 shl 1
        private const val FLAG_DENIED = 1 shl 2
        private const val MASK_PERMISSION = FLAG_ALLOWED or FLAG_DENIED
    }
}
