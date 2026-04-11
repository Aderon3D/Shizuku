package moe.shizuku.manager.core.utils

import com.topjohnwu.superuser.Shell

object RootUtils {
    fun isRooted(): Boolean =
            Shell.isAppGrantedRoot() ?:
            Shell.getShell().isRoot
}
