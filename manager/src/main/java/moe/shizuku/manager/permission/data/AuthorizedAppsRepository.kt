package moe.shizuku.manager.permission.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import moe.shizuku.manager.core.android.DeviceUserHelper
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.permission.models.App
import moe.shizuku.manager.privilegedservice.api.UserServiceManager

class AuthorizedAppsRepository(
    private val context: Context,
    private val deviceUserHelper: DeviceUserHelper,
    private val userServiceManager: UserServiceManager
) {

    suspend fun getAppsDeclaringPermission(allUsers: Boolean = true): List<App> {
        val appsList = if (allUsers) {
            getInstalledAppsForAllUsers()
        } else {
            getInstalledAppsForUser(deviceUserHelper.myUserId)
        }

        Log.d(TAG, "getApplicationLabels")
        return appsList.map {
            App(
                info = it,
                label = context.packageManager.getApplicationLabel(it).toString()
            )
        }.also {
            Log.d(TAG, "Finished getAppsDeclaringPermission")
        }
    }

    private suspend fun getInstalledAppsForAllUsers(): MutableList<ApplicationInfo> {
        val users = deviceUserHelper.getUsers().keys
        val appsList = mutableListOf<ApplicationInfo>()

        for (user in users) {
            Log.d(TAG, "getInstalledAppsForUser $user")
            val userApps = getInstalledAppsForUser(user)
            appsList.addAll(userApps)
        }

        return appsList
    }

    private suspend fun getInstalledAppsForUser(userId: Int) =
        userServiceManager.getService()
            .getInstalledApplicationsAsUser(userId)

}