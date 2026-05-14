package moe.shizuku.manager.core.utils.runnable

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import com.github.michaelbull.result.Result

abstract class Runnable<out T, out E> {
    private val _status = MutableStateFlow<RunnableStatus<T, E>>(RunnableStatus.Pending)
    val status: StateFlow<RunnableStatus<T, E>> = _status

    fun refresh() {
        _status.update { current ->
            if (current is RunnableStatus.Running) {
                RunnableStatus.Running()
            } else {
                current
            }
        }
    }

    suspend fun run(): Result<T, E> {
        _status.update { RunnableStatus.Running() }
        val result = onRun()
        _status.update { RunnableStatus.Finished(result) }
        return result
    }

    protected abstract suspend fun onRun(): Result<T, E>
}