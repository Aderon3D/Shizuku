package moe.shizuku.manager.core.extensions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import moe.shizuku.manager.R
import moe.shizuku.manager.core.platform.device.AndroidVersion
import moe.shizuku.manager.core.platform.services.systemService

private val Context.clipboardManager: ClipboardManager by systemService()

fun Context.copyToClipboard(text: String) {
    val clip = ClipData.newPlainText("text", text)
    clipboardManager.setPrimaryClip(clip)

    if (AndroidVersion.isAtLeast12v2) toast(R.string.copied_to_clipboard)
}
