package moe.shizuku.manager.core.extensions

import android.os.DeadObjectException

val Exception.isBinderDeadException: Boolean
    get() = this is IllegalStateException ||
            (this is RuntimeException && this.cause is DeadObjectException)
