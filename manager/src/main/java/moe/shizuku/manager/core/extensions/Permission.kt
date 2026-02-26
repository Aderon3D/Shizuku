package moe.shizuku.manager.core.extensions

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.Context
import android.content.pm.PackageManager

fun Context.hasPermission(permission: String) =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

fun Context.hasWriteSecureSettings() =
    hasPermission(WRITE_SECURE_SETTINGS)