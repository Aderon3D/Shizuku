package moe.shizuku.manager.start.models

import moe.shizuku.manager.core.utils.runnable.RunnableSequenceError
import moe.shizuku.manager.core.utils.runnable.RunnableStatus

data class StartUiState(
    val steps: List<StartStepItem> = emptyList(),
    val status: RunnableStatus<Unit, RunnableSequenceError> = RunnableStatus.Pending
)
