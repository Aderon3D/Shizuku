package moe.shizuku.manager.core.extensions

import android.content.res.Resources

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Int.toUserId: Int
    get() = this / 100000

val Int.isValidPort
    get() = this in 1..65535