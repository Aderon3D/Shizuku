package moe.shizuku.manager.shell

import android.content.pm.PackageManager
import android.os.IBinder
import rikka.rish.Rish
import rikka.rish.RishConfig
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants
import kotlin.system.exitProcess

class Shell : Rish() {

    override fun requestPermission(onGrantedRunnable: Runnable) {
        when {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> {
                onGrantedRunnable.run()
            }
            Shizuku.shouldShowRequestPermissionRationale() -> {
                System.err.println("Permission denied")
                System.err.flush()
                exitProcess(1)
            }
            else -> {
                Shizuku.addRequestPermissionResultListener(object : Shizuku.OnRequestPermissionResultListener {
                    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                        Shizuku.removeRequestPermissionResultListener(this)

                        if (grantResult == PackageManager.PERMISSION_GRANTED) {
                            onGrantedRunnable.run()
                        } else {
                            System.err.println("Permission denied")
                            System.err.flush()
                            exitProcess(1)
                        }
                    }
                })
                Shizuku.requestPermission(0)
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>, packageName: String, binder: IBinder) {
            RishConfig.init(binder, ShizukuApiConstants.BINDER_DESCRIPTOR, 30000)
            Shizuku.onBinderReceived(binder, packageName)
            Shizuku.addBinderReceivedListenerSticky {
                val version = Shizuku.getVersion()
                if (version < 12) {
                    System.err.println("Rish requires server 12 (running $version)")
                    System.err.flush()
                    exitProcess(1)
                }
                Shell().start(args)
            }
        }
    }
}
