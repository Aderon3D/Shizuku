package moe.shizuku.manager.core.platform.services.packages.manager

import android.content.pm.IPackageManager
import android.content.pm.IPackageManagerPre17
import android.content.pm.PackageInfo
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.mapError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shizuku.manager.core.extensions.isBinderDeadException
import moe.shizuku.manager.core.extensions.resultOf
import moe.shizuku.manager.core.extensions.unsafeCast
import moe.shizuku.manager.core.platform.device.AndroidVersion
import moe.shizuku.manager.core.platform.services.systemService
import moe.shizuku.manager.privilegedservice.PrivilegedServiceManager

class PackageInfoRepository {
    private val packageManager by systemService("package", IPackageManager.Stub::asInterface)
    private val packageManagerPre17 by lazy {
        unsafeCast<IPackageManagerPre17>(packageManager)
    }

    suspend fun getInstalledPackages(
        flags: Int,
        userId: Int
    ): Result<List<PackageInfo>, GetInstalledPackagesError> = withContext(Dispatchers.IO) {
        resultOf {
            when {
                AndroidVersion.isAtLeast17 ->
                    resultOf {
                        packageManager.getInstalledPackages(flags.toLong(), userId)
                    }.getOrElse {
                        packageManagerPre17.getInstalledPackages(flags.toLong(), userId)
                    }

                AndroidVersion.isAtLeast13 ->
                    packageManagerPre17.getInstalledPackages(flags.toLong(), userId)

                else ->
                    packageManagerPre17.getInstalledPackages(flags, userId)
            }.list
        }.mapError {
            when {
                it is SecurityException -> GetInstalledPackagesError.NoPermission
                it.isBinderDeadException -> GetInstalledPackagesError.PrivilegedServiceNotRunning
                else -> throw it
            }
        }
    }
}
