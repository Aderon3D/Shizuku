package moe.shizuku.manager.core.platform.adb

import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.provider.Settings
import androidx.annotation.CheckResult
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import moe.shizuku.manager.core.extensions.hasWriteSecureSettings
import moe.shizuku.manager.core.extensions.isTelevision
import moe.shizuku.manager.core.extensions.isWifiConnected
import moe.shizuku.manager.core.platform.adb.models.AdbSettingsError
import moe.shizuku.manager.core.platform.device.AndroidVersion
import moe.shizuku.manager.core.platform.services.KeyguardHelper
import kotlin.coroutines.resume

class AdbSettingsManager(
    private val context: Context,
    private val keyguardHelper: KeyguardHelper
) {
    // USB DEBUGGING

    val isUsbDebuggingEnabled: Boolean
        get() = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) > 0

    @CheckResult
    fun setUsbDebugging(enabled: Boolean): Result<Unit, AdbSettingsError> {
        if (isUsbDebuggingEnabled == enabled) return Ok(Unit)
        if (!context.hasWriteSecureSettings()) return Err(AdbSettingsError.NoWriteSecureSettings)

        val cr = context.contentResolver
        Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, if (enabled) 1 else 0)
        if (enabled) {
            Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
        }

        return Ok(Unit)
    }

    // WIRELESS DEBUGGING

    val hasWirelessDebugging: Boolean
        @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
        get() = if (context.isTelevision) AndroidVersion.isAtLeast14
        else AndroidVersion.isAtLeast11

    val isWirelessDebuggingEnabled: Boolean
        @RequiresApi(Build.VERSION_CODES.R)
        get() = Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) > 0

    @RequiresApi(Build.VERSION_CODES.R)
    @CheckResult
    suspend fun setWirelessDebugging(
        enabled: Boolean,
        onAwaitingAuth: (Boolean) -> Unit = {}
    ): Result<Unit, AdbSettingsError> = withContext(Dispatchers.IO) {
        if (!hasWirelessDebugging) Err(AdbSettingsError.NotSupported)

        setWirelessDebuggingInternal(enabled) ?: run {

            onAwaitingAuth(true)

            if (keyguardHelper.isKeyguardLocked) {
                keyguardHelper.waitForUnlock()
                setWirelessDebuggingInternal(true)?.let {
                    onAwaitingAuth(false)
                    it
                }
            }

            awaitAuthResult().also {
                onAwaitingAuth(false)
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @CheckResult
    private suspend fun setWirelessDebuggingInternal(enabled: Boolean): Result<Unit, AdbSettingsError>? {
        if (isWirelessDebuggingEnabled == enabled) return Ok(Unit)
        if (enabled && !context.isWifiConnected) return Err(AdbSettingsError.NoWifi)
        if (!context.hasWriteSecureSettings()) return Err(AdbSettingsError.NoWriteSecureSettings)

        Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", if (enabled) 1 else 0)

        if (!enabled) return Ok(Unit)

        // Debounce to detect network not authorized for wireless debugging
        delay(100)
        return if (isWirelessDebuggingEnabled) Ok(Unit)
        else null // Non-terminal state, need to wait for auth result
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @CheckResult
    private suspend fun awaitAuthResult(): Result<Unit, AdbSettingsError> =
        suspendCancellableCoroutine { cont ->
            val cr = context.contentResolver
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    cr.unregisterContentObserver(this)

                    val result = if (isWirelessDebuggingEnabled) Ok(Unit)
                    else Err(AdbSettingsError.NotAuthorized)

                    cont.resume(result)
                }
            }
            cr.registerContentObserver(
                Settings.Global.getUriFor("adb_wifi_enabled"),
                false,
                observer
            )
            cont.invokeOnCancellation {
                cr.unregisterContentObserver(observer)
            }
        }
}
