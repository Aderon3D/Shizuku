package moe.shizuku.manager.core.platform.adb

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import moe.shizuku.manager.core.extensions.resultOf
import moe.shizuku.manager.core.platform.adb.client.AdbClient
import moe.shizuku.manager.core.platform.adb.client.AdbKey
import moe.shizuku.manager.core.platform.adb.client.PreferenceAdbKeyStore
import moe.shizuku.manager.core.platform.adb.models.AdbConnectionError
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import java.io.EOFException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLProtocolException

class AdbSession(
    private val preferencesRepository: PreferencesRepository,
    port: Int
) : AutoCloseable {
    private val mutex = Mutex()
    private var key: AdbKey? = null
    private var client: AdbClient? = null
    private var isConnected = false

    var port: Int = port
        set(value) {
            if (field != value) {
                field = value
                closeClient()
            }
        }

    private suspend fun getKey() = key ?: withContext(Dispatchers.IO) {
        AdbKey(
            PreferenceAdbKeyStore(preferencesRepository.prefs),
            "shizuku"
        ).also { key = it }
    }

    private suspend fun createClient(): AdbClient {
        val port = this.port
        require(port > 0) { "Port must be greater than 0" }

        return AdbClient("127.0.0.1", port, getKey())
    }

    private suspend fun getClient(): AdbClient = mutex.withLock {
        client?.let { return@withLock it }

        createClient().also {
            client = it
            isConnected = false
        }
    }

    suspend fun connect(): Result<AdbClient, AdbConnectionError> {
        val client = getClient()

        return if (isConnected) Ok(client)
        else connectWithRetry(client).onOk {
            isConnected = true
        }
    }

    private suspend fun connectWithRetry(client: AdbClient): Result<AdbClient, AdbConnectionError> {
        var delayTime = 0L
        val maxAttempts = 5

        for (attempt in 1..maxAttempts) {
            if (delayTime > 0) delay(delayTime)

            resultOf { client.connect() }
                .fold(
                    success = { return Ok(client) },
                    failure = {
                        when (it) {
                            is SSLProtocolException -> return Err(AdbConnectionError.NotPaired)
                            is EOFException, is SocketTimeoutException -> {
                                if (attempt < maxAttempts) delayTime += 1000
                                else return Err(AdbConnectionError.ConnectionFailed(it))
                            }

                            is IOException -> return Err(AdbConnectionError.ConnectionFailed(it))
                            else -> throw it
                        }
                    }
                )
        }

        error("No return value after $maxAttempts attempts")
    }

    suspend fun <T> withClient(block: suspend (AdbClient) -> T): Result<T, AdbConnectionError> =
        withContext(Dispatchers.IO) {
            connect().andThen { client ->
                resultOf { block(client) }
                    .mapError {
                        if (it is IOException) {
                            closeClient()
                            AdbConnectionError.ConnectionFailed(it)
                        } else throw it
                    }
            }
        }

    private fun closeClient() {
        client?.close()
        client = null
        isConnected = false
    }

    override fun close(): Unit = closeClient()

    class Factory(
        private val preferencesRepository: PreferencesRepository
    ) {
        fun create(port: Int = 0): AdbSession =
            AdbSession(preferencesRepository, port)
    }
}
