package moe.shizuku.manager.core.platform.services

import android.content.Context
import android.os.IBinder
import android.os.IInterface
import androidx.core.content.ContextCompat
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import kotlin.properties.ReadOnlyProperty

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
        context.requireSystemService<T>()
    }

inline fun <reified T : Any> systemService() =
    ReadOnlyProperty<Context, T> { context, _ ->
        context.requireSystemService<T>()
    }

@PublishedApi
internal inline fun <reified T : Any> Context.requireSystemService(): T {
    return ContextCompat.getSystemService(this, T::class.java)
        ?: throw IllegalStateException("System service ${T::class.java.simpleName} not found")
}