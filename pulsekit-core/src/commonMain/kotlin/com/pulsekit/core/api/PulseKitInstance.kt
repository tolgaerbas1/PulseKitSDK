package com.pulsekit.core.api

import com.pulsekit.core.api.PulseKit.isInitialized
import com.pulsekit.core.api.config.PulseKitConfig
import com.pulsekit.core.api.events.EventProcessor
import com.pulsekit.core.api.events.FlagProvider
import com.pulsekit.core.api.events.PulseEvent
import com.pulsekit.core.api.events.PulseKitStatus
import com.pulsekit.core.api.events.SessionInfo
import com.pulsekit.core.api.flags.FeatureFlag
import com.pulsekit.core.api.flags.FeatureFlagManager
import com.pulsekit.core.api.flags.FlagPersistence
import com.pulsekit.core.api.networking.EventBatchSender
import com.pulsekit.core.api.networking.FeatureFlagService
import com.pulsekit.core.api.networking.NetworkClient
import com.pulsekit.core.api.session.SessionManager
import com.pulsekit.core.api.storage.EventQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The actual SDK instance that handles all operations.
 *
 * This class is internal to the SDK implementation but provides the public API
 * through its public methods. Users interact with this through PulseKit.instance.
 */
public class PulseKitInstance internal constructor(
    public val config: PulseKitConfig,
    scope: CoroutineScope? = null,
    batchSender: EventBatchSender? = null,
) : FlagProvider {
    private var initialized: Boolean = false
    private val sdkScope: CoroutineScope = scope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val eventQueue: EventQueue = EventQueue(config, sdkScope, batchSender)
    private val sessionManager: SessionManager = SessionManager(config, sdkScope)
    private val eventProcessor: EventProcessor = EventProcessor.create(config, eventQueue, sdkScope, this)

    // Feature flag system
    private val flagManager: FeatureFlagManager = FeatureFlagManager(sdkScope)

    init {
        // Wire session start/end to event queue as LifecycleEvents
        sessionManager.setOnTrackEvent(eventProcessor::process)
    }

    /**
     * Configure the feature flag system with platform-specific networking and persistence.
     *
     * Called automatically by platform initialization code (e.g. [com.pulsekit.android.PulseKitAndroid]).
     * This wires the core [FeatureFlagManager] to:
     * 1. Load persisted flags from disk
     * 2. Fetch updated flags from the server periodically
     * 3. Save server responses to disk for offline availability
     * 4. Provide an on-demand refresh mechanism via [FeatureFlagManager.setRefreshAction]
     */
    public fun configureFeatureFlags(
        networkClient: NetworkClient,
        persistence: FlagPersistence,
    ) {
        // 1. Load persisted flags into the manager
        sdkScope.launch {
            try {
                persistence.loadFlags()?.let { flags ->
                    flagManager.updateServerFlags(flags)
                    if (config.enableDebugLogging) {
                        com.pulsekit.core.api.logging.PulseKitLogger.log(
                            "PulseKit.Flags",
                            "Loaded ${flags.size} persisted feature flags",
                        )
                    }
                }
            } catch (e: Exception) {
                if (config.enableDebugLogging) {
                    com.pulsekit.core.api.logging.PulseKitLogger.log(
                        "PulseKit.Flags",
                        "Failed to load persisted flags: ${e.message}",
                    )
                }
            }
        }

        // 2. Create service and wire periodic fetching
        val service = createFlagService(networkClient)

        flagManager.setRefreshAction {
            sdkScope.launch {
                service.fetchFeatureFlags().onSuccess { response ->
                    // Persist updated flags for offline availability
                    sdkScope.launch {
                        try {
                            persistence.saveFlags(response.flags)
                        } catch (_: Exception) { }
                    }
                }
            }
        }

        // 3. Start periodic background fetching
        service.startPeriodicFetching()
    }

    /**
     * Create a [FeatureFlagService] backed by the given [NetworkClient].
     */
    private fun createFlagService(networkClient: NetworkClient): FeatureFlagService {
        return FeatureFlagService(networkClient, flagManager, sdkScope)
    }

    /**
     * Get the current value of a boolean flag.
     */
    override fun getBooleanFlag(flag: FeatureFlag): Boolean {
        return flagManager.getBooleanFlag(flag)
    }

    /**
     * Get the current value of an integer flag.
     */
    override fun getIntegerFlag(flag: FeatureFlag): Long {
        return flagManager.getIntegerFlag(flag)
    }

    /**
     * Get the current value of a double flag.
     */
    override fun getDoubleFlag(flag: FeatureFlag): Double {
        return flagManager.getDoubleFlag(flag)
    }

    /**
     * Get the current value of a string flag.
     */
    override fun getStringFlag(flag: FeatureFlag): String {
        return flagManager.getStringFlag(flag)
    }

    /**
     * Track an event.
     *
     * Events are queued locally and processed according to the SDK's offline-first strategy.
     * The method is non-blocking and returns immediately.
     *
     * @param event The event to track
     * @throws PulseKitError if the event cannot be processed
     */
    public fun track(event: PulseEvent) {
        eventProcessor.process(event)
    }

    /**
     * Get the current session information.
     *
     * @return Current session data, or null if no active session
     */
    public fun getCurrentSession(): com.pulsekit.core.api.events.SessionInfo? = sessionManager.getCurrentSessionInfo()

    /**
     * Manually start a new session.
     *
     * This is typically handled automatically by lifecycle observers,
     * but can be called manually for custom session management.
     */
    public fun startSession() {
        sessionManager.startNewSession()
    }

    /**
     * End the current session.
     *
     * This will flush any pending events and mark the session as completed.
     */
    public fun endSession() {
        sessionManager.endCurrentSession()
    }

    /**
     * Force flush all queued events.
     *
     * This attempts to immediately send all queued events to the server.
     * The operation is asynchronous and may not complete immediately.
     */
    public fun flush() {
        eventQueue.flush()
    }

    /**
     * Get SDK status information.
     *
     * Useful for debugging and monitoring SDK health.
     */
    public fun getStatus(): PulseKitStatus {
        return if (isInitialized) {
            PulseKitStatus.READY
        } else {
            PulseKitStatus.INITIALIZING
        }
    }

    /**
     * Shutdown the SDK instance.
     *
     * This will cancel all ongoing operations and clean up resources.
     * After calling this, the SDK must be re-initialized to be used again.
     */
    internal fun shutdown() {
        flagManager.cleanup()
        sdkScope.cancel()
        eventQueue.clear()
        sessionManager.cleanup()
    }
}
