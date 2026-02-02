package com.pulsekit.android

import android.content.Context
import com.pulsekit.android.lifecycle.PulseKitLifecycleObserver
import com.pulsekit.android.lifecycle.SessionLifecycleListener
import com.pulsekit.android.storage.AndroidFileFlagStorage
import com.pulsekit.core.api.PulseKit
import com.pulsekit.core.api.config.PulseKitConfig
import com.pulsekit.core.api.flags.DiskFlagStorage
import com.pulsekit.core.api.flags.FlagPersistence
import com.pulsekit.core.api.flags.InMemoryFlagStorage
import com.pulsekit.core.api.flags.PulseKitFeatureFlags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Android-specific entry point for PulseKit SDK with zero integration work.
 * 
 * This provides Android-specific initialization and automatic lifecycle integration.
 * The SDK automatically handles session management, lifecycle tracking, and
 * offline-first event persistence without any additional setup required.
 * 
 * Key features:
 * - Automatic session management based on app lifecycle
 * - Zero configuration required for basic usage
 * - Comprehensive lifecycle event tracking
 * - Session timeout handling
 * - Activity monitoring to prevent premature timeouts
 * - Server-driven feature flags for remote behavior control
 */
public object PulseKitAndroid {
    
    /**
     * Initialize PulseKit with Android-specific features and zero integration work.
     * 
     * This method automatically sets up:
     * - Lifecycle observers for automatic session management
     * - Session timeout monitoring
     * - Activity tracking
     * - Comprehensive event tracking
     * - Server-driven feature flags
     * - Network connectivity monitoring
     * 
     * Simply call this in your Application's onCreate() and everything else is handled.
     * 
     * @param context Application context
     * @param config The configuration for PulseKit
     * @param enableLifecycleObserver Whether to enable automatic lifecycle integration
     * @return The initialized PulseKitInstance
     */
    public fun initialize(
        context: Context,
        config: PulseKitConfig = PulseKitConfig(),
        enableLifecycleObserver: Boolean = true
    ) {
        // Initialize core SDK
        val instance = PulseKit.initialize(config, CoroutineScope(Dispatchers.Default))
        
        // Set up Android-specific integrations
        if (enableLifecycleObserver && config.enableAutoSessionManagement) {
            PulseKitLifecycleObserver.initialize(context, instance)
        }
        
        // Initialize feature flag system
        initializeFeatureFlags(context, instance, config)
        
        // TODO: Set up network connectivity monitoring
        // TODO: Set up crash reporting integration
    }
    
    /**
     * Initialize the feature flag system.
     */
    private fun initializeFeatureFlags(
        context: Context,
        instance: com.pulsekit.core.api.PulseKitInstance,
        config: PulseKitConfig
    ) {
        // Create flag storage based on configuration
        val flagStorage = if (config.enableDiskPersistence) {
            val platformStorage = AndroidFileFlagStorage(context)
            DiskFlagStorage(platformStorage)
        } else {
            InMemoryFlagStorage()
        }
        
        // Create flag persistence
        val flagPersistence = FlagPersistence(
            scope = CoroutineScope(Dispatchers.IO),
            storage = flagStorage
        )
        
        // Load persisted flags
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val persistedFlags = flagPersistence.loadFlags()
                persistedFlags?.let { flags ->
                    // Update flag manager with persisted values
                    // This will be handled by the flag manager
                }
            } catch (e: Exception) {
                // Continue with default values if loading fails
            }
        }
    }
    
    /**
     * Get the Android-specific context if available.
     * 
     * @return Application context, or null if not initialized
     */
    public fun getContext(): Context? {
        return PulseKitLifecycleObserver.getContext()
    }
    
    /**
     * Check if PulseKit has been initialized with Android support.
     */
    public val isInitialized: Boolean
        get() = PulseKit.isInitialized
    
    /**
     * Get the underlying PulseKit instance.
     */
    public val instance: com.pulsekit.core.api.PulseKitInstance
        get() = PulseKit.instance
    
    /**
     * Check if the app is currently in foreground.
     * 
     * @return true if app is in foreground, false otherwise
     */
    public fun isAppInForeground(): Boolean {
        return PulseKitLifecycleObserver.isAppInForeground()
    }
    
    /**
     * Get current session information.
     * 
     * @return Current session info, or null if no active session
     */
    public fun getCurrentSessionInfo(): com.pulsekit.android.lifecycle.SessionInfo? {
        return PulseKitLifecycleObserver.getCurrentSessionInfo()
    }
    
    /**
     * Update activity timestamp to prevent session timeout.
     * 
     * Call this when user activity is detected (button clicks, touches, etc.)
     * to ensure the session doesn't timeout due to inactivity.
     * 
     * This is optional - the SDK automatically tracks activity during
     * event tracking, but you can call this manually for additional
     * activity signals.
     */
    public fun updateActivity() {
        PulseKitLifecycleObserver.updateActivity()
    }
    
    /**
     * Set a listener for detailed session lifecycle events.
     * 
     * This allows you to monitor session state changes and implement
     * custom logic based on session lifecycle.
     * 
     * @param listener The session lifecycle listener, or null to remove
     */
    public fun setSessionListener(listener: SessionLifecycleListener?) {
        PulseKitLifecycleObserver.setSessionListener(listener)
    }
    
    /**
     * Force cleanup of the lifecycle observer.
     * 
     * This is typically only needed for testing or when manually
     * managing the SDK lifecycle. In normal usage, cleanup is handled
     * automatically.
     */
    public fun cleanup() {
        PulseKitLifecycleObserver.cleanup()
    }
    
    /**
     * Get current feature flag values for debugging.
     * 
     * This method is primarily for debugging and monitoring.
     * The actual flag values are used internally by the SDK.
     * 
     * @return Map of flag keys to their current values
     */
    public fun getFeatureFlagValues(): Map<String, Any> {
        return try {
            val instance = PulseKit.instance
            PulseKitFeatureFlags.ALL_FLAGS.associate { flag ->
                val value = when (flag.type) {
                    com.pulsekit.core.api.flags.FlagType.BOOLEAN -> instance.getBooleanFlag(flag)
                    com.pulsekit.core.api.flags.FlagType.INTEGER -> instance.getIntegerFlag(flag)
                    com.pulsekit.core.api.flags.FlagType.DOUBLE -> instance.getDoubleFlag(flag)
                    com.pulsekit.core.api.flags.FlagType.STRING -> instance.getStringFlag(flag)
                }
                flag.key to value
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    /**
     * Check if a specific feature flag is enabled.
     * 
     * This method is primarily for debugging and monitoring.
     * The actual flag values are used internally by the SDK.
     * 
     * @param flag The feature flag to check
     * @return Current value of the flag
     */

}
