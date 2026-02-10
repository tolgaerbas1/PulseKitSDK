package com.pulsekit.core.api

import com.pulsekit.core.api.PulseKit.isInitialized
import com.pulsekit.core.api.config.PulseKitConfig
import com.pulsekit.core.api.events.EventProcessor
import com.pulsekit.core.api.events.FlagProvider
import com.pulsekit.core.api.events.PulseEvent
import com.pulsekit.core.api.events.SessionInfo
import com.pulsekit.core.api.events.PulseKitStatus
import com.pulsekit.core.api.session.SessionManager
import com.pulsekit.core.api.storage.EventQueue
import com.pulsekit.core.api.flags.FeatureFlagManager
import com.pulsekit.core.api.flags.FeatureFlag
import com.pulsekit.core.api.flags.PulseKitFeatureFlags
import com.pulsekit.core.api.networking.EventBatchSender
import com.pulsekit.core.api.networking.FeatureFlagService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
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
    batchSender: EventBatchSender? = null
) : FlagProvider {
    private var initialized: Boolean = false
    private val sdkScope: CoroutineScope = scope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val eventQueue: EventQueue = EventQueue(config, sdkScope, batchSender)
    private val sessionManager: SessionManager = SessionManager(config, sdkScope)
    private val eventProcessor: EventProcessor = EventProcessor.create(config, eventQueue, sdkScope, this)
    
    // Feature flag system
    private val flagManager: FeatureFlagManager = FeatureFlagManager(sdkScope)
    private val flagService: FeatureFlagService? = createFlagService()
    
    init {
        // Wire session start/end to event queue as LifecycleEvents
        sessionManager.setOnTrackEvent(eventProcessor::process)
        // Initialize feature flags
        initializeFeatureFlags()
        // Start periodic flag fetching
        flagService?.startPeriodicFetching()
    }
    
    /**
     * Create feature flag service.
     * This will be implemented by platform-specific modules.
     */
    private fun createFlagService(): FeatureFlagService? {
        // This will be overridden by Android implementation
        return null
    }
    
    /**
     * Initialize feature flag system.
     */
    private fun initializeFeatureFlags() {
        // Load persisted flags if available
        sdkScope.launch {
            try {
                // This will be implemented by platform-specific modules
                loadPersistedFlags()
            } catch (e: Exception) {
                // Continue with default values if loading fails
            }
        }
    }
    
    /**
     * Load persisted flags (platform-specific).
     */
    private fun loadPersistedFlags() {
        // This will be implemented by platform-specific modules
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
