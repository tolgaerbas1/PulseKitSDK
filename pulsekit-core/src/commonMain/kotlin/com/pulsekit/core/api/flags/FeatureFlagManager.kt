package com.pulsekit.core.api.flags

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

/**
 * Internal feature flag manager.
 * 
 * This class handles evaluation of feature flags with server overrides
 * while maintaining fast, thread-safe access and fallback to defaults.
 * 
 * This is completely internal to the SDK and not exposed to users.
 */
internal class FeatureFlagManager(
    private val scope: CoroutineScope
) {
    
    private val flagValues = mutableMapOf<String, FlagValue>()
    private val lastFetchTime = MutableStateFlow<Instant?>(null)
    private val fetchStatus = MutableStateFlow<FetchStatus>(FetchStatus.NOT_FETCHED)
    
    // Cache for server flag values
    private var serverFlags: Map<String, FlagValue> = emptyMap()
    private var serverFlagsTimestamp: Instant? = null
    
    // Cache expiration time (5 minutes)
    private val cacheExpiration = 5.minutes
    
    init {
        // Initialize with default values
        PulseKitFeatureFlags.ALL_FLAGS.forEach { flag ->
            flagValues[flag.key] = flag.defaultValue
        }
    }
    
    /**
     * Get the current value of a boolean flag.
     */
    fun getBooleanFlag(flag: FeatureFlag): Boolean {
        val value = getFlagValue(flag)
        return when (value) {
            is FlagValue.BooleanValue -> value.value
            else -> {
                // Type mismatch - log and return default
                logTypeError(flag, "Boolean", value)
                when (flag.defaultValue) {
                    is FlagValue.BooleanValue -> flag.defaultValue.value
                    else -> false // Safe fallback
                }
            }
        }
    }
    
    /**
     * Get the current value of an integer flag.
     */
    fun getIntegerFlag(flag: FeatureFlag): Long {
        val value = getFlagValue(flag)
        return when (value) {
            is FlagValue.IntegerValue -> value.value
            else -> {
                // Type mismatch - log and return default
                logTypeError(flag, "Integer", value)
                when (flag.defaultValue) {
                    is FlagValue.IntegerValue -> flag.defaultValue.value
                    else -> 0L // Safe fallback
                }
            }
        }
    }
    
    /**
     * Get the current value of a double flag.
     */
    fun getDoubleFlag(flag: FeatureFlag): Double {
        val value = getFlagValue(flag)
        return when (value) {
            is FlagValue.DoubleValue -> value.value
            else -> {
                // Type mismatch - log and return default
                logTypeError(flag, "Double", value)
                when (flag.defaultValue) {
                    is FlagValue.DoubleValue -> flag.defaultValue.value
                    else -> 0.0 // Safe fallback
                }
            }
        }
    }
    
    /**
     * Get the current value of a string flag.
     */
    fun getStringFlag(flag: FeatureFlag): String {
        val value = getFlagValue(flag)
        return when (value) {
            is FlagValue.StringValue -> value.value
            else -> {
                // Type mismatch - log and return default
                logTypeError(flag, "String", value)
                when (flag.defaultValue) {
                    is FlagValue.StringValue -> flag.defaultValue.value
                    else -> "" // Safe fallback
                }
            }
        }
    }
    
    /**
     * Get the current value of a flag (type-safe).
     */
    private fun getFlagValue(flag: FeatureFlag): FlagValue {
        // Check if cache is expired
        if (isCacheExpired()) {
            // Return cached value while fetching fresh data
            refreshServerFlags()
        }
        
        // Return server value if available, otherwise default
        return serverFlags[flag.key] ?: flagValues[flag.key] ?: flag.defaultValue
    }
    
    /**
     * Update flags from server response.
     * 
     * This is called internally by the networking layer.
     */
    fun updateServerFlags(flags: Map<String, FlagValue>) {
        serverFlags = flags
        serverFlagsTimestamp = Clock.System.now()
        lastFetchTime.value = serverFlagsTimestamp
        fetchStatus.value = FetchStatus.SUCCESS
        
        // Track flag fetch success
        trackFlagFetch(true, flags.size)
    }
    
    /**
     * Handle flag fetch failure.
     */
    fun handleFetchFailure(error: Throwable) {
        fetchStatus.value = FetchStatus.FAILED
        lastFetchTime.value = Clock.System.now()
        
        // Track flag fetch failure
        trackFlagFetch(false, 0, error)
    }
    
    /**
     * Force refresh server flags.
     */
    fun refreshServerFlags() {
        if (fetchStatus.value == FetchStatus.FETCHING) {
            return // Already fetching
        }
        
        fetchStatus.value = FetchStatus.FETCHING
        
        // This will be handled by the networking layer
        scope.launch {
            try {
                // Signal networking layer to fetch flags
                // The actual implementation will be in the networking module
                refreshServerFlagsInternal()
            } catch (e: Exception) {
                handleFetchFailure(e)
            }
        }
    }
    
    /**
     * Internal method to refresh server flags.
     * This will be implemented by the networking module.
     */
    private fun refreshServerFlagsInternal() {
        // Placeholder - actual implementation in networking module
        // This will be overridden by the Android-specific implementation
    }
    
    /**
     * Check if the cache is expired.
     */
    private fun isCacheExpired(): Boolean {
        val timestamp = serverFlagsTimestamp ?: return true
        return Clock.System.now() - timestamp > cacheExpiration
    }
    
    /**
     * Get the fetch status.
     */
    fun getFetchStatus(): StateFlow<FetchStatus> = fetchStatus.asStateFlow()
    
    /**
     * Get the last fetch time.
     */
    fun getLastFetchTime(): StateFlow<Instant?> = lastFetchTime.asStateFlow()
    
    /**
     * Get all current flag values (for debugging).
     */
    fun getAllFlagValues(): Map<String, FlagValue> {
        return PulseKitFeatureFlags.ALL_FLAGS.associate { flag ->
            flag.key to getFlagValue(flag)
        }
    }
    
    /**
     * Get active experimental flags.
     */
    fun getActiveExperimentalFlags(): List<String> {
        return PulseKitFeatureFlags.EXPERIMENTAL_FLAGS.filter { flag ->
            when (val value = getFlagValue(flag)) {
                is FlagValue.BooleanValue -> value.value
                else -> false
            }
        }.map { it.key }
    }
    
    /**
     * Log type errors for debugging.
     */
    private fun logTypeError(flag: FeatureFlag, expectedType: String, actualValue: FlagValue) {
        // This would use the SDK's logging system
        // For now, we'll just note the issue
        println("Feature flag type error: ${flag.key} expected $expectedType but got ${actualValue::class.simpleName}")
    }
    
    /**
     * Track flag fetch metrics.
     */
    private fun trackFlagFetch(success: Boolean, flagCount: Int, error: Throwable? = null) {
        // This would send telemetry about flag fetching
        // For now, we'll just log it
        if (success) {
            println("Feature flags fetched successfully: $flagCount flags")
        } else {
            println("Feature flags fetch failed: ${error?.message}")
        }
    }
    
    /**
     * Cleanup resources.
     */
    fun cleanup() {
        // Clear caches and cancel any ongoing operations
        serverFlags = emptyMap()
        serverFlagsTimestamp = null
        fetchStatus.value = FetchStatus.NOT_FETCHED
        lastFetchTime.value = null
    }
}

/**
 * Status of flag fetching operations.
 */
internal enum class FetchStatus {
    NOT_FETCHED,
    FETCHING,
    SUCCESS,
    FAILED
}
