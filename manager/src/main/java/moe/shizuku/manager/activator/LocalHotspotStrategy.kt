package moe.shizuku.manager.activator

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume

/**
 * Strategy 2: Local-only hotspot to create a network interface.
 *
 * On OEM devices that refuse to start adbd on TCP without an active
 * network (Samsung, Xiaomi, OnePlus), this strategy creates a local-only
 * hotspot via the public Android API WifiManager.startLocalOnlyHotspot().
 *
 * This creates a local LAN interface on the device, which satisfies the
 * network check that adbd performs before starting on TCP.
 *
 * The local-only hotspot:
 * - Does NOT share internet
 * - Does NOT require mobile data
 * - Is available to regular (non-system) apps via the public API
 * - Requires CHANGE_WIFI_STATE (normal, auto-granted) and
 *   ACCESS_FINE_LOCATION (on Android 12-13, runtime-granted)
 */
class LocalHotspotStrategy(private val context: Context) : ActivatorStrategy {

    companion object {
        private const val TAG = "LocalHotspotStrategy"
        private const val POLL_ATTEMPTS = 15
        private const val POLL_DELAY_MS = 1000L
        private const val HOTSPOT_TIMEOUT_MS = 15_000L

        @Volatile
        var hotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null
    }

    override val name: String get() = "local_hotspot"

    override val description: String get() =
        "Create local hotspot, then enable wireless ADB"

    override suspend fun activate(log: ((String) -> Unit)?): Int {
        if (!isAvailable()) {
            log?.invoke("Local hotspot not available (missing permissions or API level)")
            return -1
        }

        log?.invoke("Starting local-only hotspot...")

        val hotspotStarted = startLocalOnlyHotspot(log)
        if (!hotspotStarted) {
            log?.invoke("Failed to start local-only hotspot")
            return -1
        }

        log?.invoke("Hotspot is running, enabling wireless debugging...")

        // Enable ADB now that we have a network interface
        withContext(Dispatchers.IO) {
            Settings.Global.putInt(context.contentResolver, Settings.Global.ADB_ENABLED, 1)
            Settings.Global.putLong(context.contentResolver, "adb_allowed_connection_time", 0L)
            Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", 1)
        }

        log?.invoke("Waiting for ADB daemon to start on TCP...")

        // Poll for ADB port
        for (i in 1..POLL_ATTEMPTS) {
            delay(POLL_DELAY_MS)

            val port = AdbPortScanner.scan()
            if (port > 0) {
                log?.invoke("ADB daemon detected on port $port")
                return port
            }

            if (i % 5 == 0) {
                log?.invoke("Still waiting for ADB... (attempt ${i}/$POLL_ATTEMPTS)")
            }
        }

        log?.invoke("ADB daemon did not start on TCP within timeout")
        return -1
    }

    override suspend fun isAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false

        // CHANGE_WIFI_STATE is a normal permission, auto-granted
        // ACCESS_FINE_LOCATION needed on Android 12-13 for hotspot discovery
        val hasLocationPermission = if (Build.VERSION.SDK_INT in 31..33) {
            ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true

        return hasLocationPermission
    }

    /**
     * Start local-only hotspot via the public WifiManager API.
     * Uses suspendCancellableCoroutine to bridge the callback-based API.
     */
    private suspend fun startLocalOnlyHotspot(log: ((String) -> Unit)?): Boolean {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return false

        return try {
            suspendCancellableCoroutine { continuation ->
                wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                        Log.d(TAG, "Local-only hotspot started")
                        log?.invoke("Local-only hotspot is active")
                        // Store reservation so it stays alive during activation
                        hotspotReservation = reservation
                        continuation.resume(true)
                    }

                    override fun onFailed(reason: Int) {
                        Log.w(TAG, "Local-only hotspot failed: reason=$reason")
                        log?.invoke("Hotspot failed (reason=$reason)")
                        continuation.resume(false)
                    }
                }, null)

                // Timeout safeguard
                continuation.invokeOnCancellation {
                    hotspotReservation?.close()
                    hotspotReservation = null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting local-only hotspot", e)
            log?.invoke("Error starting hotspot: ${e.message}")
            false
        }
    }

    /**
     * Clean up the hotspot reservation.
     */
    fun stopHotspot() {
        hotspotReservation?.close()
        hotspotReservation = null
    }
}
