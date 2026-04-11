package moe.shizuku.manager.core.platform.services.pkg

import android.content.pm.IPackageManager
import android.content.pm.IPackageManagerPre17
import android.content.pm.PackageInfo
import android.util.Log
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.unsafeCast
import moe.shizuku.manager.core.platform.device.AndroidVersion
import moe.shizuku.manager.core.platform.services.systemService

class PackageInfoRepository {
    private val packageManager by systemService("package", IPackageManager.Stub::asInterface)
    private val packageManagerPre17 by lazy {
        unsafeCast<IPackageManagerPre17>(packageManager)
    }

    fun getInstalledPackages(flags: Int, userId: Int): List<PackageInfo> = runCatching {
        Log.d(TAG, "getInstalledApplicationsAsUser: $userId")
        getInstalledPackagesCompat(flags, userId)
    }.onFailure {
        Log.e(TAG, "getInstalledApplicationsAsUser: $userId", it)
    }.getOrDefault(emptyList())

    private fun getInstalledPackagesCompat(flags: Int, userId: Int): List<PackageInfo> {
        val parceledList = when {
            AndroidVersion.isAtLeast17 ->
                runCatching {
                    packageManager.getInstalledPackages(flags.toLong(), userId)
                }.recover {
                    packageManagerPre17.getInstalledPackages(flags.toLong(), userId)
                }.getOrThrow()

            AndroidVersion.isAtLeast13 ->
                packageManagerPre17.getInstalledPackages(flags.toLong(), userId)

            else ->
                packageManagerPre17.getInstalledPackages(flags, userId)
        }

        return parceledList.list
    }
}