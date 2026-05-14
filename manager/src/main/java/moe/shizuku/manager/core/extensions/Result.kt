package moe.shizuku.manager.core.extensions

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.CancellationException

// Modified runCatching from library
// Catches Exception instead of Throwable
// Rethrows CancellationException

inline fun <V> resultOf(block: () -> V): Result<V, Exception> {
    return try {
        Ok(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Err(e)
    }
}

inline infix fun <T, V> T.resultOf(block: T.() -> V): Result<V, Exception> {
    return try {
        Ok(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Err(e)
    }
}