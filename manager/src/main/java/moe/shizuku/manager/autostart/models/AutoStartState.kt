package moe.shizuku.manager.autostart.models

sealed class AutoStartState {
    sealed class Waiting : AutoStartState() {
        data object Wifi : Waiting()
        data object Retry : Waiting()
        data object FirstRun : Waiting()
    }
    data class Running(val isAwaitingAuth: Boolean) : AutoStartState()
    object Success : AutoStartState()
    object Cancelled : AutoStartState()

    val isFinished = this is Success || this is Cancelled
}