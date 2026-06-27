package moe.shizuku.manager.activator

/**
 * Interface for ADB activation strategies.
 *
 * Each strategy represents a different approach to starting the ADB
 * daemon (adbd) on a TCP port. Strategies are tried in order by
 * [ActivatorEngine] until one succeeds.
 */
interface ActivatorStrategy {

    /**
     * Human-readable name for logging and display.
     */
    val name: String

    /**
     * Short description of what this strategy does.
     */
    val description: String

    /**
     * Attempt to activate ADB on a TCP port.
     *
     * @param log Optional callback for progress messages.
     * @return The TCP port where ADB is listening, or -1 on failure.
     */
    suspend fun activate(log: ((String) -> Unit)? = null): Int

    /**
     * Check if this strategy can run on the current device.
     * (e.g., required permissions are granted)
     */
    suspend fun isAvailable(): Boolean = true
}
