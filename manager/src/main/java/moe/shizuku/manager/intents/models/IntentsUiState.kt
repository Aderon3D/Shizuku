package moe.shizuku.manager.intents.models

import moe.shizuku.manager.BuildConfig

data class IntentsUiState(
    val enabled: Boolean,
    val intentAction: IntentAction,
    val authToken: String
) {
    enum class IntentAction {
        START, STOP
    }
}