package moe.shizuku.manager.core.utils.root

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RootUtils {
    fun isRooted(): Boolean? =
        Shell.isAppGrantedRoot()

    suspend fun requestRootPermission(): Result<Unit, RootError> = withContext(Dispatchers.IO) {
        val isPermissionGranted = Shell.getShell().isRoot

        if (isPermissionGranted) Ok(Unit)
        else Err(RootError.PermissionDenied)
    }
}