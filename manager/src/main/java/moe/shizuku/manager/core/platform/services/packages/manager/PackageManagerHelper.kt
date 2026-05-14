package moe.shizuku.manager.core.platform.services.packages.manager

import android.content.Context
import android.content.pm.PackageManager
import com.github.michaelbull.result.fold
import moe.shizuku.manager.core.extensions.resultOf

class PackageManagerHelper(private val context: Context) {
    private val packageManager by lazy {
        context.packageManager
    }

    val packageName: String by lazy {
        context.packageName
    }

    fun isPackageInstalled(pkgName: String): Boolean = resultOf {
        packageManager.getPackageInfo(pkgName, 0)
    }.fold(
        success = { true },
        failure = { if (it is PackageManager.NameNotFoundException) false else throw it }
    )
}