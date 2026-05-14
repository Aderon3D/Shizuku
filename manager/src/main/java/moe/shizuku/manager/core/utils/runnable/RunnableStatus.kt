package moe.shizuku.manager.core.utils.runnable

import com.github.michaelbull.result.Result

sealed interface RunnableStatus<out T, out E> {
    data object Pending : RunnableStatus<Nothing, Nothing>
    class Running : RunnableStatus<Nothing, Nothing>
    data class Finished<out T, out E>(val result: Result<T, E>) : RunnableStatus<T, E>
}