package moe.shizuku.manager.core.platform.services.user

import android.content.pm.UserInfo
import android.os.IUserManager
import android.system.Os
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.toUserId
import moe.shizuku.manager.core.platform.device.AndroidVersion
import moe.shizuku.manager.core.platform.services.systemService

class DeviceUserRepository {
    private var userCache: Map<Int, DeviceUser> = emptyMap()
    private val mutex = Mutex()

    private val userManager by systemService("user", IUserManager.Stub::asInterface)

    private val currentUserId = Os.getuid().toUserId

    private fun isCurrentUser(userId: Int) = userId == currentUserId

    fun getCurrentUser(): DeviceUser =
        userCache[currentUserId] ?: createPlaceholder(currentUserId)

    suspend fun getUser(userId: Int): DeviceUser {
        if (userCache.isEmpty()) getUsers()
        return userCache[userId] ?: createPlaceholder(userId)
    }

    suspend fun getUsers(): Set<DeviceUser> = mutex.withLock {
        if (userCache.isEmpty()) {
            runCatching {
                userCache = getUsersCompat().associate {
                    it.id to it.toDeviceUser()
                }
            }.onFailure {
                Log.w(TAG, "getUsers", it)
            }
        }

        return@withLock userCache.values.toSet()
    }

    private fun getUsersCompat() : List<UserInfo> = runCatching {
        if (AndroidVersion.isAtLeast11) {
            userManager.getUsers(true, true, true)
        } else {
            userManager.getUsers(true)
        }
    }.recoverCatching {
        userManager.getUsers(true)
    }.onFailure {
        Log.e(TAG, "getUsers", it)
    }.getOrDefault(emptyList())

    private fun createPlaceholder(userId: Int) = DeviceUser(
        id = userId,
        name = "User $userId",
        isCurrentUser = isCurrentUser(userId),
    )

    private fun UserInfo.toDeviceUser() = DeviceUser(
        id = this.id,
        name = this.name,
        isCurrentUser = isCurrentUser(this.id),
    )
}