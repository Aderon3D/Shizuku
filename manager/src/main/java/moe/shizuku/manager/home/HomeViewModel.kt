package moe.shizuku.manager.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import moe.shizuku.manager.core.platform.services.BatteryOptimizationHelper
import moe.shizuku.manager.core.platform.services.packages.manager.PackageManagerHelper
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import moe.shizuku.manager.core.utils.ApkUtils
import moe.shizuku.manager.home.models.HelpItem
import moe.shizuku.manager.home.models.HomeEvent
import moe.shizuku.manager.home.models.HomeUiState
import moe.shizuku.manager.home.models.PrivilegedServiceUiState
import moe.shizuku.manager.permission.PermissionManager
import moe.shizuku.manager.permission.data.AuthorizedAppsRepository
import moe.shizuku.manager.privilegedservice.PrivilegedServiceStateMachine
import moe.shizuku.manager.privilegedservice.ServiceMetadataRepository
import moe.shizuku.manager.privilegedservice.models.PrivilegedServiceState
import moe.shizuku.manager.tcpmode.TcpManager
import moe.shizuku.manager.tcpmode.models.TcpState
import moe.shizuku.manager.watchdog.WatchdogManager

class HomeViewModel(
    authorizedAppsRepository: AuthorizedAppsRepository,
    privilegedServiceStateMachine: PrivilegedServiceStateMachine,
    tcpManager: TcpManager,
    watchdogManager: WatchdogManager,
    private val batteryOptimizationHelper: BatteryOptimizationHelper,
    private val packageManagerHelper: PackageManagerHelper,
    private val permissionManager: PermissionManager,
    private val preferencesRepository: PreferencesRepository,
    private val serviceMetadataRepository: ServiceMetadataRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        privilegedServiceStateMachine.state,
        watchdogManager.state,
        tcpManager.tcpState,
        authorizedAppsRepository.grantedCount
    ) { serviceState, watchdogState, tcpState, grantedCount ->
        HomeUiState(
            serviceState = when (serviceState) {
                is PrivilegedServiceState.Running -> {
                    val mode =
                        if (serviceState.uid == 0) PrivilegedServiceUiState.Running.Mode.ROOT
                        else PrivilegedServiceUiState.Running.Mode.ADB

                    PrivilegedServiceUiState.Running(
                        mode = mode,
                        version = "v${serviceState.version}",
                        isLatestVersion = serviceMetadataRepository.isLatestVersion().getOr(true),
                        canGrantPermission = permissionManager.canGrantPermission().getOr(true)
                    )
                }

                else -> PrivilegedServiceUiState.Stopped
            },
            watchdogState = watchdogState,
            tcpState = tcpState,
            isStealthModeActive = !packageManagerHelper.isPackageInstalled(ApkUtils.ORIGINAL_PACKAGE_NAME),
            authorizedAppsCount = grantedCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(
            serviceState = PrivilegedServiceUiState.Stopped,
            watchdogState = watchdogManager.state.value,
            tcpState = TcpState(TcpState.Port.Disabled, TcpState.Port.Disabled),
            isStealthModeActive = !packageManagerHelper.isPackageInstalled(ApkUtils.ORIGINAL_PACKAGE_NAME),
            authorizedAppsCount = 0
        )
    )

    private val _events = Channel<HomeEvent>()
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    fun checkPermissionOwner() {
        permissionManager.isPermissionOwner()
            .onOk { isOwner ->
                if (!isOwner) {
                    _events.trySend(HomeEvent.ShowUninstallDialog)
                }
            }
            .onErr {
                _events.trySend(HomeEvent.ShowRebootDialog)
            }
    }

    fun handleSelectionResult(value: Any) {
        when (value) {
            is HelpItem -> onHelpItemSelected(value)
        }
    }

    private fun onHelpItemSelected(item: HelpItem) {
        val url = when (item) {
            HelpItem.USER_GUIDE -> "https://github.com/thedjchi/Shizuku/wiki"
            HelpItem.TROUBLESHOOTING -> "https://github.com/thedjchi/Shizuku/wiki/troubleshooting"
            HelpItem.BUG_REPORT -> "https://github.com/thedjchi/Shizuku/issues/new?template=bug_report.yml"
            HelpItem.FEATURE_REQUEST -> "https://github.com/thedjchi/Shizuku/issues/new?template=feature_request.yml"
            HelpItem.TRANSLATE -> "https://crowdin.com/project/shizuku"
            HelpItem.EMAIL -> "mailto:thedjchidev@gmail.com"
            HelpItem.PRIVACY -> "https://github.com/thedjchi/Shizuku?tab=readme-ov-file#-privacy"
        }
        _events.trySend(HomeEvent.OpenUrl(url))
    }

    fun checkBatteryOptimization() {
        val applicableSettings = setOf(
            preferencesRepository.startOnBoot,
            preferencesRepository.watchdog
        )
        val areAnyEnabled = applicableSettings.any { it.get() }
        if (!areAnyEnabled) return

        if (!batteryOptimizationHelper.isIgnoringBatteryOptimizations) {
            _events.trySend(HomeEvent.ShowBatteryOptimizationSnackbar)
        }
    }

}
