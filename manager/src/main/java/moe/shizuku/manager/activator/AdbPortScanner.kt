package moe.shizuku.manager.activator

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Scans localhost ports for an active ADB daemon (adbd) listener.
 *
 * ADB wire protocol check: reads the first 24 bytes of the connect message.
 * A valid ADB port responds with A_CNXN (0x4e584e43) as the first int
 * in little-endian.
 *
 * Strategy: first tries the last-known-good port, then scans range 37000-44000
 * (Android 11+ wireless debugging range), then tries the TCP mode port 5555.
 */
object AdbPortScanner {

    private const val TAG = "AdbPortScanner"
    private const val ADB_CNXN_MAGIC = 0x4e584e43 // "CNXN" in little-endian

    // Android 11+ wireless debugging port range
    private const val PORT_RANGE_START = 37000
    private const val PORT_RANGE_END = 44000

    // Standard tcpip port
    private const val TCP_MODE_PORT = 5555

    // Connection timeout per port (ms)
    private const val CONNECT_TIMEOUT = 300L
    private const val READ_TIMEOUT = 500L

    @Volatile
    var lastKnownPort: Int = -1
        private set

    /**
     * Scan for an ADB port on localhost.
     * Returns the port number, or -1 if no ADB daemon was found.
     */
    suspend fun scan(): Int = withContext(Dispatchers.IO) {
        // 1. Try last-known port first
        if (lastKnownPort > 0 && probePort(lastKnownPort)) {
            Log.d(TAG, "Found ADB on last-known port $lastKnownPort")
            return@withContext lastKnownPort
        }

        // 2. Try TCP mode port 5555
        if (probePort(TCP_MODE_PORT)) {
            Log.d(TAG, "Found ADB on TCP mode port $TCP_MODE_PORT")
            lastKnownPort = TCP_MODE_PORT
            return@withContext TCP_MODE_PORT
        }

        // 3. Scan wireless debugging range
        val found = scanRange(PORT_RANGE_START, PORT_RANGE_END)
        if (found > 0) {
            Log.d(TAG, "Found ADB on port $found")
            lastKnownPort = found
            return@withContext found
        }

        Log.d(TAG, "No ADB port found on localhost")
        -1
    }

    /**
     * Scan a range of ports. Uses efficient batching.
     */
    private fun scanRange(start: Int, end: Int): Int {
        // Scan in batches to avoid overwhelming the system
        for (port in start..end) {
            if (probePort(port)) return port
        }
        return -1
    }

    /**
     * Probe a single port for ADB protocol handshake.
     */
    private fun probePort(port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", port), CONNECT_TIMEOUT.toInt())
                socket.soTimeout = READ_TIMEOUT.toInt()

                val input = socket.getInputStream()
                val header = ByteArray(24)
                val read = input.read(header)

                if (read < 24) return@use false

                val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
                val command = buffer.int
                command == ADB_CNXN_MAGIC
            }
        } catch (e: EOFException) {
            false
        } catch (e: IOException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Quick check: is ADB running on any known port?
     * Less thorough than scan() but much faster (checks last known + 5555 only).
     */
    suspend fun quickCheck(): Boolean = withContext(Dispatchers.IO) {
        val ports = listOfNotNull(
            lastKnownPort.takeIf { it > 0 },
            TCP_MODE_PORT
        ).distinct()
        ports.any { probePort(it) }
    }

    /**
     * Reset the cached last-known port.
     */
    fun reset() {
        lastKnownPort = -1
    }
}
