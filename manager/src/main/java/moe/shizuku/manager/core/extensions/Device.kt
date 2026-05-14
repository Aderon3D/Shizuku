package moe.shizuku.manager.core.extensions

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import moe.shizuku.manager.core.platform.services.systemService

private val Context.uiModeManager: UiModeManager by systemService()

val Context.isWatch: Boolean
    get() = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_WATCH


val Context.isTelevision: Boolean
    get() = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
            || packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)