package moe.shizuku.manager.core.utils.runnable

sealed interface RunnableSequenceError {
    data class StepFailed<T : Runnable<*, *>>(val step: T) : RunnableSequenceError
}