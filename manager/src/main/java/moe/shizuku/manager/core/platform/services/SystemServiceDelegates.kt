package moe.shizuku.manager.core.platform.services

import android.content.Context
import android.os.IBinder
import android.os.IInterface
import androidx.core.content.ContextCompat
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

inline fun <reified T : IInterface> systemService(
    name: String,
    crossinline stubAsInterface: (IBinder) -> T
) = lazy(LazyThreadSafetyMode.NONE) {
        val service = SystemServiceHelper.getSystemService(name)
        val binder = ShizukuBinderWrapper(service)
        stubAsInterface(binder)
    }

inline fun <reified T : Any> systemService(context: Context) =
    lazy(LazyThreadSafetyMode.NONE) {
        ContextCompat.getSystemService(context, T::class.java)
            ?: throw IllegalStateException("System service ${T::class.java.simpleName} not found")
    }