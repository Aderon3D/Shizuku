package moe.shizuku.manager.core.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.collection.LruCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import me.zhanghai.android.appiconloader.AppIconLoader
import java.util.concurrent.Executors

class AppIconCache(private val context: Context) {
    private class AppIconLruCache(maxSize: Int) :
        LruCache<String, Bitmap>(maxSize) {

        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    private val lruCache: LruCache<String, Bitmap>
    private val dispatcher: CoroutineDispatcher
    private val appIconLoaders = mutableMapOf<Int, AppIconLoader>()

    init {
        val maxMemory = Runtime.getRuntime().maxMemory() / 1024
        val availableCacheSize = (maxMemory / 4).toInt()
        lruCache = AppIconLruCache(availableCacheSize)

        val threadCount = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        dispatcher = Executors.newFixedThreadPool(threadCount).asCoroutineDispatcher()
    }

    private fun getCacheKey(packageName: String, userId: Int, size: Int): String {
        return "$packageName:$userId:$size"
    }

    suspend fun loadIcon(info: ApplicationInfo, userId: Int, size: Int): Bitmap? = withContext(dispatcher) {
        val key = getCacheKey(info.packageName, userId, size)
        lruCache[key]?.let { return@withContext it }

        val loader = synchronized(appIconLoaders) {
            appIconLoaders.getOrPut(size) {
                val atLeast30 = Build.VERSION.SDK_INT >= 30
                val shrinkNonAdaptiveIcons =
                    atLeast30 && context.packageManager.let { pm ->
                        runCatching { context.applicationInfo.loadIcon(pm) is AdaptiveIconDrawable }.getOrDefault(false)
                    }
                AppIconLoader(size, shrinkNonAdaptiveIcons, context)
            }
        }

        runCatching {
            loader.loadIcon(info, false).also {
                lruCache.put(key, it)
            }
        }.getOrNull()
    }
}