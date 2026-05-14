package moe.shizuku.manager.core.platform.device

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

@Suppress("unused")
object AndroidVersion {
    private val sdkVersion = Build.VERSION.SDK_INT

    @ChecksSdkIntAtLeast(api = 37)
    val isAtLeast17: Boolean = sdkVersion >= 37

    @ChecksSdkIntAtLeast(api = 36)
    val isAtLeast16: Boolean = sdkVersion >= 36

    @ChecksSdkIntAtLeast(api = 35)
    val isAtLeast15: Boolean = sdkVersion >= 35

    @ChecksSdkIntAtLeast(api = 34)
    val isAtLeast14: Boolean = sdkVersion >= 34

    @ChecksSdkIntAtLeast(api = 33)
    val isAtLeast13: Boolean = sdkVersion >= 33

    @ChecksSdkIntAtLeast(api = 32)
    val isAtLeast12v2: Boolean = sdkVersion >= 32

    @ChecksSdkIntAtLeast(api = 31)
    val isAtLeast12: Boolean = sdkVersion >= 31

    @ChecksSdkIntAtLeast(api = 30)
    val isAtLeast11: Boolean = sdkVersion >= 30

    @ChecksSdkIntAtLeast(api = 29)
    val isAtLeast10: Boolean = sdkVersion >= 29

    @ChecksSdkIntAtLeast(api = 28)
    val isAtLeast9: Boolean = sdkVersion >= 28

    @ChecksSdkIntAtLeast(api = 26)
    val isAtLeast8: Boolean = sdkVersion >= 26
}