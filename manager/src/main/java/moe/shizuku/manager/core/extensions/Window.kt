package moe.shizuku.manager.core.extensions

import android.graphics.Color
import android.os.Build
import android.view.Window
import androidx.core.view.WindowInsetsControllerCompat
import moe.shizuku.manager.core.platform.device.AndroidVersion

private val darkScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
private val lightScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)

fun Window.setNavBarScrim(scrimEnabled: Boolean) {
    val isNightMode = context.resources.isNightMode

    // Light nav bar icons are not supported below API 26, so we must always show a dark scrim
    if (!AndroidVersion.isAtLeast8) {
        @Suppress("DEPRECATION")
        navigationBarColor = darkScrim
        return
    }

    // On API 29+, isNavigationBarContrastEnforced sets the correct scrim color based on navigation and dark mode
    // However, transparent button navigation bar requires disabling it and setting the light/dark icons manually

    // On API 28 and lower, everything must be set manually

    if (AndroidVersion.isAtLeast10) {
        isNavigationBarContrastEnforced = scrimEnabled
    } else {
        @Suppress("DEPRECATION")
        navigationBarColor = if (scrimEnabled) {
            if (isNightMode) darkScrim
            else lightScrim
        } else Color.TRANSPARENT
    }

    WindowInsetsControllerCompat(this, decorView).run {
        isAppearanceLightNavigationBars = !isNightMode
    }
}