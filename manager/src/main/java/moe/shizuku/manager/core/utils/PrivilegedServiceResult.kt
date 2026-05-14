package moe.shizuku.manager.core.utils

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import moe.shizuku.manager.core.extensions.isBinderDeadException
import moe.shizuku.manager.core.extensions.resultOf
import moe.shizuku.manager.privilegedservice.models.PrivilegedServiceError

fun <T> resultOfPrivilegedService(block: () -> T): Result<T, PrivilegedServiceError> =
    resultOf { block() }
        .mapError {
            if (it.isBinderDeadException) PrivilegedServiceError.NotRunning
            else throw it
        }