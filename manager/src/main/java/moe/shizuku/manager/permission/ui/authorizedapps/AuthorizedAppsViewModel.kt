package moe.shizuku.manager.permission.ui.authorizedapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onErr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.permission.data.AuthorizedAppsRepository
import moe.shizuku.manager.permission.models.AuthorizedAppsEvent
import moe.shizuku.manager.permission.models.AuthorizedAppsItem
import moe.shizuku.manager.permission.models.AuthorizedAppsUiState
import moe.shizuku.manager.privilegedservice.PrivilegedServiceStateMachine
import rikka.shizuku.Shizuku

class AuthorizedAppsViewModel(
    private val authorizedAppsRepository: AuthorizedAppsRepository,
    stateMachine: PrivilegedServiceStateMachine,
) : ViewModel() {
    private val _isRefreshing = MutableStateFlow(false)


    val uiState: StateFlow<AuthorizedAppsUiState> = combine(
        authorizedAppsRepository.appsList,
        stateMachine.isRunningFlow,
        _isRefreshing
    ) { list, running, refreshing ->
        if (list == null) {
            AuthorizedAppsUiState.Loading
        } else {
            AuthorizedAppsUiState.Success(
                apps = list,
                isServiceRunning = running,
                isRefreshing = refreshing
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuthorizedAppsUiState.Loading
    )

    private val _events = Channel<AuthorizedAppsEvent>()
    val events: Flow<AuthorizedAppsEvent> = _events.receiveAsFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            authorizedAppsRepository.getAuthorizedApps()
            _isRefreshing.value = false
        }
    }

    fun toggleApp(app: AuthorizedAppsItem.App) {
        viewModelScope.launch {
            authorizedAppsRepository.updatePermission(app, !app.isGranted).onErr {
                if (it is SecurityException) handleSecurityException()
                else _events.trySend(
                    AuthorizedAppsEvent.ShowError(R.string.authorized_apps_error_toggle)
                )
            }
        }
    }

    fun toggleAll(grant: Boolean) {
        val state = uiState.value

        if (state !is AuthorizedAppsUiState.Success) return

        viewModelScope.launch {
            var anyFailed = false
            var securityExceptionOccurred = false

            val updatedApps = mutableListOf<AuthorizedAppsItem.App>()

            for (app in state.apps) {
                if (securityExceptionOccurred || app.isGranted == grant) {
                    updatedApps.add(app)
                    continue
                }

                authorizedAppsRepository.updatePermission(app, grant).onErr {
                    if (it is SecurityException) {
                        securityExceptionOccurred = true
                    } else {
                        anyFailed = true
                    }
                    updatedApps.add(app)
                }
            }

            if (securityExceptionOccurred) {
                handleSecurityException()
            } else if (anyFailed) {
                _events.trySend(AuthorizedAppsEvent.ShowError(R.string.authorized_apps_error_toggle_all))
            }
        }
    }

    private fun handleSecurityException() {
        val uid = runCatching {
            Shizuku.getUid()
        }.getOrDefault(-1)
        if (uid > 0) {
            _events.trySend(AuthorizedAppsEvent.NotifyAdbRestricted)
        }
    }
}
