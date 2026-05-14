package moe.shizuku.manager.core.platform.services.packages.installer

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import java.io.File

class PackageInstallerHelper(private val context: Context) {

    private var callback: ((Boolean, String?) -> Unit)? = null

    private val installerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
            val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

            when (status) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    val confirmationIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_INTENT)
                    }
                    if (confirmationIntent != null) {
                        context.startActivity(confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
                else -> {
                    val isSuccess = (status == PackageInstaller.STATUS_SUCCESS)
                    callback?.invoke(isSuccess, msg)
                    try {
                        context.unregisterReceiver(this)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        }
    }

    @SuppressLint("RequestInstallPackagesPolicy")
    fun install(apk: File, cb: ((Boolean, String?) -> Unit)? = null) {
        val installer = context.packageManager.packageInstaller

        val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = installer.createSession(sessionParams)
        val session = installer.openSession(sessionId)

        apk.inputStream().use { input ->
            session.openWrite("base.apk", 0, apk.length()).use { output ->
                input.copyTo(output)
                session.fsync(output)
            }
        }

        val pendingIntent = createInstallerPendingIntent(sessionId, cb)

        session.commit(pendingIntent.intentSender)
        session.close()
    }

    fun uninstall(pkgName: String, cb: ((Boolean, String?) -> Unit)? = null) {
        val installer = context.packageManager.packageInstaller
        val pendingIntent = createInstallerPendingIntent(0, cb)
        installer.uninstall(pkgName, pendingIntent.intentSender)
    }

    private fun createInstallerPendingIntent(
        sessionId: Int,
        cb: ((Boolean, String?) -> Unit)? = null
    ): PendingIntent {
        callback = cb

        val installerAction = "${context.packageName}.INSTALLER_RESULT"
        val filter = IntentFilter().apply {
            addAction(installerAction)
        }

        val receiverFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_NOT_EXPORTED
        } else {
            0
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(installerReceiver, filter, receiverFlags)
        } else {
            context.registerReceiver(installerReceiver, filter)
        }

        val callbackIntent = Intent(installerAction).apply {
            setPackage(context.packageName)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getBroadcast(context, sessionId, callbackIntent, flags)
    }
}