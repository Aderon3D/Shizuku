package moe.shizuku.manager.core.platform.services.user

import android.content.pm.UserInfo
import android.os.IUserManager
import android.system.Os
import android.util.Log
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onOk
import com.github.michaelbull.result.toErrorIfNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.isBinderDeadException
import moe.shizuku.manager.core.extensions.resultOf
import moe.shizuku.manager.core.extensions.toUserId
import moe.shizuku.manager.core.platform.device.AndroidVersion
import moe.shizuku.manager.core.platform.services.systemService
import moe.shizuku.manager.core.platform.services.user.models.DeviceUser
import moe.shizuku.manager.core.platform.services.user.models.GetUsersError

class DeviceUserRepository(
    private val scope: CoroutineScope
) {
    private var userCache: Map<Int, DeviceUser>? = null
    private val mutex = Mutex()

    private val userManager by systemService("user", IUserManager.Stub::asInterface)

    private val currentUserId = Os.getuid().toUserId

    private fun isCurrentUser(userId: Int) = userId == currentUserId

    fun getCurrentUser(): DeviceUser =
        userCache?.get(currentUserId) ?: createPlaceholder(currentUserId)

    suspend fun getUser(userId: Int): DeviceUser =
        getUsers()
            .map { users -> users.find { it.id == userId } }
            .toErrorIfNull { GetUsersError.NotFound }
            .getOr(createPlaceholder(userId))


    suspend fun getUsers(): Result<List<DeviceUser>, GetUsersError> =
        mutex.withLock {
            userCache?.let {
                scope.launch { getAndCacheUsers() }
                Ok(it.values.toList())
            }

            getAndCacheUsers()
        }

    private suspend fun getAndCacheUsers(): Result<List<DeviceUser>, GetUsersError> =
        getUsersFromSystem().onOk { users ->
            userCache = users.associateBy { it.id }
        }

    private suspend fun getUsersFromSystem(): Result<List<DeviceUser>, GetUsersError> =
        resultOf {
            withContext(Dispatchers.IO) {
                try {
                    if (AndroidVersion.isAtLeast11) userManager.getUsers(true, true, true)
                    else userManager.getUsers(true)
                } catch (_: NoSuchMethodError) {
                    // Some devices or Android versions require the simpler API call
                    userManager.getUsers(true)
                }
            }
        }.map { userInfos ->
            userInfos.map { it.toDeviceUser() }
        }.mapError {
            Log.e(TAG, "Failed to get users from system", it)
            when {
                it is SecurityException -> GetUsersError.NoPermission
                it.isBinderDeadException -> GetUsersError.PrivilegedServiceNotRunning
                else -> throw it
            }
        }

    private fun createPlaceholder(userId: Int) = DeviceUser(
        id = userId,
        name = "User $userId", // TODO localize
        isCurrentUser = isCurrentUser(userId),
    )

    private fun UserInfo.toDeviceUser() = DeviceUser(
        id = this.id,
        name = this.name,
        isCurrentUser = isCurrentUser(this.id),
    )
}
