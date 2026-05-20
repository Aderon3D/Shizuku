package moe.shizuku.manager

import android.app.Application
import com.topjohnwu.superuser.Shell
import moe.shizuku.manager.core.di.appModule
import moe.shizuku.manager.core.locale.data.LocaleMigrator
import moe.shizuku.manager.core.platform.device.AndroidVersion
import moe.shizuku.manager.core.platform.services.notifications.NotificationChannelManager
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.lsposed.hiddenapibypass.HiddenApiBypass

class MainApplication : Application() {
    private val localeMigrator: LocaleMigrator by inject()
    private val notificationChannelHelper: NotificationChannelManager by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            workManagerFactory()
            modules(appModule)
        }

        init()
    }

    private fun init() {
        Shell.setDefaultBuilder(Shell.Builder.create())
        if (AndroidVersion.isAtLeast9) {
            HiddenApiBypass.setHiddenApiExemptions("")
        }
        if (AndroidVersion.isAtLeast11) {
            System.loadLibrary("adb")
        }

        localeMigrator.migrate()
        notificationChannelHelper.createChannels()
    }
}
