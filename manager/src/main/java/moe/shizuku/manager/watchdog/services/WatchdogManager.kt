package moe.shizuku.manager.watchdog.services

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import moe.shizuku.manager.core.data.preferences.PreferencesRepository

object WatchdogManager {
    fun init(context: Context, scope: CoroutineScope) {
        PreferencesRepository.observeWatchdog().onEach {
            if (it) {
                WatchdogService.start(context)
            } else {
                WatchdogService.stop(context)
            }
        }.launchIn(scope)
    }
}