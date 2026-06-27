package moe.shizuku.manager.activator

import android.content.Context
import android.util.Log

/**
 * Result of an activation attempt.
 */
data class ActivationResult(
    val success: Boolean,
    val port: Int = -1,
    val strategyName: String = "",
    val error: String? = null
)

/**
 * Orchestrator that runs activation strategies in priority order.
 *
 * Strategy chain:
 * 1. DirectAdbStrategy — set adb_wifi_enabled via WRITE_SECURE_SETTINGS
 *    (works on stock AOSP/Pixel/Motorola)
 * 2. LocalHotspotStrategy — create local-only hotspot, then enable ADB
 *    (works on Samsung, Xiaomi, OnePlus that check for network)
 * 3. GuidedFallbackStrategy — ask user to manually enable hotspot/Wi-Fi
 *    (last resort, always works with user cooperation)
 *
 * Each strategy has a timeout. If it fails, the engine moves to the next.
 */
class ActivatorEngine(private val context: Context) {

    companion object {
        private const val TAG = "ActivatorEngine"
        private const val STRATEGY_TIMEOUT_MS = 45_000L
    }

    private val strategies: List<ActivatorStrategy> = listOf(
        DirectAdbStrategy(context),
        LocalHotspotStrategy(context),
        GuidedFallbackStrategy(context)
    )

    /**
     * Run the activation chain.
     *
     * @param log Callback for progress messages.
     * @return ActivationResult with the outcome.
     */
    suspend fun activate(log: ((String) -> Unit)? = null): ActivationResult {
        log?.invoke("=== Shizuku Activator ===")
        log?.invoke("Starting ADB activation...")

        for (strategy in strategies) {
            log?.invoke("\n--- Strategy: ${strategy.name} ---")
            log?.invoke(strategy.description)

            if (!strategy.isAvailable()) {
                log?.invoke("⚠ Strategy not available (missing requirements)")
                continue
            }

            val result = runStrategyWithTimeout(strategy, log)
            if (result.success) {
                log?.invoke("\n✓ ADB activated via '${strategy.name}' on port ${result.port}")
                return result
            }

            log?.invoke("✗ Strategy '${strategy.name}' failed: ${result.error ?: "unknown"}")
        }

        log?.invoke("\n✗ All activation strategies failed")
        return ActivationResult(false, error = "All strategies failed to activate ADB")
    }

    private suspend fun runStrategyWithTimeout(
        strategy: ActivatorStrategy,
        log: ((String) -> Unit)?
    ): ActivationResult {
        return try {
            kotlinx.coroutines.withTimeout(STRATEGY_TIMEOUT_MS) {
                val port = strategy.activate(log)
                if (port > 0) {
                    ActivationResult(true, port, strategy.name)
                } else {
                    ActivationResult(false, error = "Returned no port")
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.w(TAG, "Strategy '${strategy.name}' timed out")
            ActivationResult(false, error = "Timed out after ${STRATEGY_TIMEOUT_MS}ms")
        } catch (e: Exception) {
            Log.e(TAG, "Strategy '${strategy.name}' threw exception", e)
            ActivationResult(false, error = "${e::class.simpleName}: ${e.message}")
        }
    }
}
