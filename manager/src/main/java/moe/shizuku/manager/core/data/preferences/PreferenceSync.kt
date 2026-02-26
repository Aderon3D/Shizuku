package moe.shizuku.manager.core.data.preferences

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import moe.shizuku.manager.core.android.receivers.BootCompleteReceiver

object PreferenceSync {
    fun init(context: Context, scope: CoroutineScope) {
        PreferencesRepository.observeStartOnBoot().onEach {
            setBootReceiverEnabled(context, it)
        }.launchIn(scope)
    }

    fun isBootReceiverEnabled(context: Context): Boolean {
        val bootCompleteReceiver = ComponentName(context, BootCompleteReceiver::class.java)
        val state = context.packageManager.getComponentEnabledSetting(bootCompleteReceiver)

        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    private fun setBootReceiverEnabled(context: Context, enabled: Boolean) {
        val bootCompleteReceiver = ComponentName(context, BootCompleteReceiver::class.java)
        val state = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED

        context.packageManager.setComponentEnabledSetting(
            bootCompleteReceiver, state, PackageManager.DONT_KILL_APP
        )
    }
}