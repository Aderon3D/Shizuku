package moe.shizuku.manager.core.utils.runnable

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class Runnable {
    private val _status = MutableStateFlow<RunnableStatus>(RunnableStatus.Pending)
    val status: StateFlow<RunnableStatus> = _status

    fun refresh() {
        _status.value = RunnableStatus.Pending
    }

    suspend fun run() {
        _status.value = RunnableStatus.Running
        try {
            onRun()
            _status.value = RunnableStatus.Completed
        } catch (t: Throwable) {
            _status.value = RunnableStatus.Failed(t)
            throw t
        }
    }

    abstract suspend fun onRun()
}