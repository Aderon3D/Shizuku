package moe.shizuku.manager.core.utils.runnable

import android.util.Log
import com.github.michaelbull.result.Err
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import moe.shizuku.manager.core.extensions.TAG
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.Ok

class RunnableSequence<out T : Runnable<*, *>>(
    private val _steps: List<T>
) : Runnable<Unit, RunnableSequenceError>() {
    val steps: Flow<List<T>> =
        combine(_steps.map { it.status }) { _steps.toList() }

    override suspend fun onRun(): Result<Unit, RunnableSequenceError> {
        for (step in _steps) {
            Log.i(TAG, "Running step: ${step::class.simpleName}")
            val result = step.run()
            if (result.isErr) return Err(RunnableSequenceError.StepFailed(step))
        }
        return Ok(Unit)
    }
}
