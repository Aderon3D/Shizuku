package moe.shizuku.manager.privilegedservice

import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import moe.shizuku.manager.core.platform.adb.AdbSettingsManager
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import moe.shizuku.manager.privilegedservice.models.PrivilegedServiceState
import rikka.shizuku.Shizuku

class PrivilegedServiceStateMachine(
    private val adbSettingsManager: AdbSettingsManager,
    private val preferencesRepository: PreferencesRepository,
    private val privilegedServiceMetadataRepository: ServiceMetadataRepository
) {
    private val _state: MutableStateFlow<PrivilegedServiceState> = MutableStateFlow(PrivilegedServiceState.Stopped)
    val state: StateFlow<PrivilegedServiceState> = _state.asStateFlow()

    val isRunningFlow: Flow<Boolean> = state.map { it is PrivilegedServiceState.Running }
        .distinctUntilChanged()

    val isRunning: Boolean
        get() = state.value is PrivilegedServiceState.Running

    init {
        refresh()

        Shizuku.addBinderReceivedListenerSticky {
            _state.update { getRunningState() }
        }

        Shizuku.addBinderDeadListener {
            onDead()
            _state.update { currentState ->
                when (currentState) {
                    is PrivilegedServiceState.Running -> PrivilegedServiceState.Crashed
                    PrivilegedServiceState.Stopping -> PrivilegedServiceState.Stopped
                    else -> currentState
                }
            }
        }
    }

    fun setStarting() {
        _state.update { PrivilegedServiceState.Starting }
    }

    fun setStoppping() {
        _state.update { PrivilegedServiceState.Stopping }
    }

    // TODO use broadcast receiver instead for reliability
    private fun onDead() {
        if (preferencesRepository.autoDisableUsbDebugging.get()) {
            adbSettingsManager.setUsbDebugging(false)
        }
    }

    fun refresh(): PrivilegedServiceState {
        val isBinderAlive = Shizuku.pingBinder()
        val newState =
            if (isBinderAlive) getRunningState()
            else PrivilegedServiceState.Stopped

        return newState.also { _state.update { it } }
    }

    private fun getRunningState(): PrivilegedServiceState {
        val uid = privilegedServiceMetadataRepository.getUid()
            .getOrElse { return PrivilegedServiceState.Stopped }
        val version = privilegedServiceMetadataRepository.getCurrentVersion()
            .getOrElse { return PrivilegedServiceState.Stopped }

        return PrivilegedServiceState.Running(
            uid = uid,
            version = version
        )
    }
}