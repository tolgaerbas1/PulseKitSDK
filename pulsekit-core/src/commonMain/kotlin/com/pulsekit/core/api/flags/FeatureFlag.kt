package com.pulsekit.core.api.flags

import kotlinx.serialization.Serializable

/**
 * Internal feature flag definition.
 * 
 * Feature flags are internal-only and not exposed to SDK users.
 * They allow remote control of SDK behavior without API changes.
 */
@Serializable
internal data class FeatureFlag(
    /**
     * Unique identifier for the flag.
     */
    val key: String,
    
    /**
     * Default value when server is unavailable.
     */
    val defaultValue: FlagValue,
    
    /**
     * Description of what the flag controls.
     */
    val description: String,
    
    /**
     * Expected data type for this flag.
     */
    val type: FlagType,
    
    /**
     * Whether this flag is experimental.
     */
    val isExperimental: Boolean = false
)

/**
 * Types of feature flag values.
 */
internal enum class FlagType {
    BOOLEAN,
    INTEGER,
    DOUBLE,
    STRING
}

/**
 * Value types for feature flags.
 */
internal sealed class FlagValue {
    data class BooleanValue(val value: Boolean) : FlagValue()
    data class IntegerValue(val value: Long) : FlagValue()
    data class DoubleValue(val value: Double) : FlagValue()
    data class StringValue(val value: String) : FlagValue()
}

/**
 * Predefined feature flags for PulseKit.
 * 
 * These flags control internal SDK behavior and are not exposed to users.
 */
internal object PulseKitFeatureFlags {
    
    /**
     * Controls the maximum batch size for event sending.
     * Default: 50 events
     */
    val EVENT_BATCH_SIZE = FeatureFlag(
        key = "event_batch_size",
        defaultValue = FlagValue.IntegerValue(50L),
        description = "Maximum number of events to send in a single batch",
        type = FlagType.INTEGER
    )
    
    /**
     * Controls whether event compression is enabled.
     * Default: true
     */
    val EVENT_COMPRESSION = FeatureFlag(
        key = "event_compression",
        defaultValue = FlagValue.BooleanValue(true),
        description = "Enable compression of event payloads",
        type = FlagType.BOOLEAN
    )
    
    /**
     * Controls the maximum retry attempts for failed events.
     * Default: 3 attempts
     */
    val MAX_RETRY_ATTEMPTS = FeatureFlag(
        key = "max_retry_attempts",
        defaultValue = FlagValue.IntegerValue(3L),
        description = "Maximum number of retry attempts for failed events",
        type = FlagType.INTEGER
    )
    
    /**
     * Controls whether exponential backoff is enabled for retries.
     * Default: true
     */
    val EXPONENTIAL_BACKOFF = FeatureFlag(
        key = "exponential_backoff",
        defaultValue = FlagValue.BooleanValue(true),
        description = "Enable exponential backoff for retry logic",
        type = FlagType.BOOLEAN
    )
    
    /**
     * Controls the session timeout duration in minutes.
     * Default: 30 minutes
     */
    val SESSION_TIMEOUT_MINUTES = FeatureFlag(
        key = "session_timeout_minutes",
        defaultValue = FlagValue.IntegerValue(30L),
        description = "Session timeout duration in minutes",
        type = FlagType.INTEGER
    )
    
    /**
     * Controls whether offline queueing is enabled.
     * Default: true
     */
    val OFFLINE_QUEUEING = FeatureFlag(
        key = "offline_queueing",
        defaultValue = FlagValue.BooleanValue(true),
        description = "Enable offline event queueing",
        type = FlagType.BOOLEAN
    )
    
    /**
     * Controls the maximum queue size for offline events.
     * Default: 1000 events
     */
    val MAX_QUEUE_SIZE = FeatureFlag(
        key = "max_queue_size",
        defaultValue = FlagValue.IntegerValue(1000L),
        description = "Maximum number of events to queue offline",
        type = FlagType.INTEGER
    )
    
    /**
     * Controls whether disk persistence is enabled.
     * Default: true
     */
    val DISK_PERSISTENCE = FeatureFlag(
        key = "disk_persistence",
        defaultValue = FlagValue.BooleanValue(true),
        description = "Enable disk persistence for events",
        type = FlagType.BOOLEAN
    )
    
    /**
     * Controls the flush interval in minutes.
     * Default: 5 minutes
     */
    val FLUSH_INTERVAL_MINUTES = FeatureFlag(
        key = "flush_interval_minutes",
        defaultValue = FlagValue.IntegerValue(5L),
        description = "Automatic flush interval in minutes",
        type = FlagType.INTEGER
    )
    
    /**
     * Controls whether debug logging is enabled.
     * Default: false
     */
    val DEBUG_LOGGING = FeatureFlag(
        key = "debug_logging",
        defaultValue = FlagValue.BooleanValue(false),
        description = "Enable debug logging",
        type = FlagType.BOOLEAN
    )
    
    /**
     * Controls whether network monitoring is enabled.
     * Default: true
     */
    val NETWORK_MONITORING = FeatureFlag(
        key = "network_monitoring",
        defaultValue = FlagValue.BooleanValue(true),
        description = "Enable network connectivity monitoring",
        type = FlagType.BOOLEAN
    )
    
    /**
     * Controls experimental retry logic.
     * Default: false (experimental)
     */
    val EXPERIMENTAL_RETRY_LOGIC = FeatureFlag(
        key = "experimental_retry_logic",
        defaultValue = FlagValue.BooleanValue(false),
        description = "Enable experimental retry logic",
        type = FlagType.BOOLEAN,
        isExperimental = true
    )
    
    /**
     * Controls experimental event deduplication.
     * Default: false (experimental)
     */
    val EVENT_DEDUPLICATION = FeatureFlag(
        key = "event_deduplication",
        defaultValue = FlagValue.BooleanValue(false),
        description = "Enable experimental event deduplication",
        type = FlagType.BOOLEAN,
        isExperimental = true
    )
    
    /**
     * All feature flags for easy iteration.
     */
    val ALL_FLAGS = listOf(
        EVENT_BATCH_SIZE,
        EVENT_COMPRESSION,
        MAX_RETRY_ATTEMPTS,
        EXPONENTIAL_BACKOFF,
        SESSION_TIMEOUT_MINUTES,
        OFFLINE_QUEUEING,
        MAX_QUEUE_SIZE,
        DISK_PERSISTENCE,
        FLUSH_INTERVAL_MINUTES,
        DEBUG_LOGGING,
        NETWORK_MONITORING,
        EXPERIMENTAL_RETRY_LOGIC,
        EVENT_DEDUPLICATION
    )
    
    /**
     * Get all experimental flags.
     */
    val EXPERIMENTAL_FLAGS = ALL_FLAGS.filter { it.isExperimental }
    
    /**
     * Get all stable flags.
     */
    val STABLE_FLAGS = ALL_FLAGS.filter { !it.isExperimental }
}
