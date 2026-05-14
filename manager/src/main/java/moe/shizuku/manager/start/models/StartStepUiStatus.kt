package moe.shizuku.manager.start.models

import moe.shizuku.manager.core.ui.helpers.UiText

sealed interface StartStepUiStatus {
    data object Pending : StartStepUiStatus
    data object Running : StartStepUiStatus
    data class Completed(val message: UiText? = null) : StartStepUiStatus
    data class Failed(
        val message: UiText,
        val actionText: String? = null,
        val action: (() -> Unit)? = null
    ) : StartStepUiStatus
}