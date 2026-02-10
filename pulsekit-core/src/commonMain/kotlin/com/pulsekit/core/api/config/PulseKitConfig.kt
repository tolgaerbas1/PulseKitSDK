package com.pulsekit.core.api.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours
import com.pulsekit.core.api.backpressure.BackpressureConfig
import com.pulsekit.core.api.backpressure.DropPolicy

/**
 * Configuration for PulseKit SDK.
 * 
 * Use the DSL builder pattern for clean configuration:
 * ```kotlin
 * val config = PulseKitConfig {
 *     apiKey = "your-api-key"
 *     enableDebugLogging = true
 *     sessionTimeout = 30.minutes
 * }
 * ```
 */
public data class PulseKitConfig(
    /**
     * API key for authentication with the PulseKit backend.
     * Required for production use.
     */
    public val apiKey: String? = null,
    
    /**
     * Base URL for the PulseKit API endpoints.
     * Defaults to the production PulseKit servers.
     */
    public val baseUrl: String = "https://api.pulsekit.dev",
    
    /**
     * Enable debug logging for development and troubleshooting.
     * Should be disabled in production builds.
     */
    public val enableDebugLogging: Boolean = false,
    
    /**
     * Maximum number of events to queue locally before forcing a flush.
     * Prevents excessive memory usage and ensures timely data delivery.
     */
    public val maxQueueSize: Int = 1000,
    
    /**
     * Time interval between automatic batch flushes.
     * Events are automatically sent in batches at this interval.
     */
    public val flushInterval: Duration = 5.minutes,
    
    /**
     * Maximum age of events before they are discarded.
     * Prevents sending stale data when offline for extended periods.
     */
    public val maxEventAge: Duration = 24.hours,
    
    /**
     * Session timeout duration.
     * If no events are tracked for this duration, the session is considered ended.
     */
    public val sessionTimeout: Duration = 30.minutes,
    
    /**
     * Enable automatic session management.
     * When true, sessions are automatically started/ended based on app lifecycle.
     */
    public val enableAutoSessionManagement: Boolean = true,
    
    /**
     * Enable offline-first mode.
     * When true, events are queued locally when offline and sent when connectivity is restored.
     */
    public val enableOfflineQueueing: Boolean = true,
    
    /**
     * Enable disk persistence for events.
     * When true, events are stored on disk and persist across app restarts.
     * Requires enableOfflineQueueing to be true.
     */
    public val enableDiskPersistence: Boolean = true,
    
    /**
     * Maximum database size in bytes before automatic cleanup.
     * When exceeded, oldest events will be removed to free space.
     */
    public val maxDatabaseSize: Long = 50 * 1024 * 1024, // 50MB
    
    /**
     * Database cleanup interval.
     * How often to run cleanup tasks like removing expired events.
     */
    public val databaseCleanupInterval: Duration = 1.hours,
    
    /**
     * Custom user agent string for API requests.
     * Useful for identifying different app versions or platforms.
     */
    public val userAgent: String? = null,
    
    /**
     * Additional metadata to include with all events.
     * Useful for app-specific context that should be tracked globally.
     */
    public val globalMetadata: Map<String, String> = emptyMap(),
    
    /**
     * Backpressure configuration for queue management.
     * Controls how events are dropped when queues become full.
     */
    val backpressureConfig: BackpressureConfig = BackpressureConfig()
) {
    
    public companion object {
        /**
         * Create a default configuration for development.
         */
        public fun development(): PulseKitConfig = PulseKitConfig(
            enableDebugLogging = true,
            enableAutoSessionManagement = true,
            enableOfflineQueueing = true
        )
        
        /**
         * Create a default configuration for production.
         */
        public fun production(): PulseKitConfig = PulseKitConfig(
            enableDebugLogging = false,
            enableAutoSessionManagement = true,
            enableOfflineQueueing = true
        )
    }
}

/**
 * DSL builder for PulseKitConfig.
 */
public inline fun PulseKitConfig(block: PulseKitConfigBuilder.() -> Unit): PulseKitConfig =
    PulseKitConfigBuilder().apply(block).build()

/**
 * Builder class for PulseKitConfig DSL.
 */
@Suppress("TooManyFunctions")
public class PulseKitConfigBuilder {
    public var apiKey: String? = null
    public var baseUrl: String = "https://api.pulsekit.dev"
    public var enableDebugLogging: Boolean = false
    public var maxQueueSize: Int = 1000
    public var flushInterval: Duration = 5.minutes
    public var maxEventAge: Duration = 24.hours
    public var sessionTimeout: Duration = 30.minutes
    public var enableAutoSessionManagement: Boolean = true
    public var enableOfflineQueueing: Boolean = true
    public var enableDiskPersistence: Boolean = true
    public var maxDatabaseSize: Long = 50 * 1024 * 1024 // 50MB
    public var databaseCleanupInterval: Duration = 1.hours
    public var userAgent: String? = null
    public var globalMetadata: MutableMap<String, String> = mutableMapOf()
    
    // Backpressure configuration
    public var maxInMemoryQueueSize: Int = 1000
    public var maxDiskQueueSize: Int = 10000
    public var dropPolicy: DropPolicy = DropPolicy.DROP_OLDEST
    public var enablePriorityDropping: Boolean = true
    public var backpressureThreshold: Double = 0.9
    public var dropWhenDiskFull: Boolean = true
    public var maxEventRetries: Int = 3

    public fun metadata(key: String, value: String) {
        globalMetadata[key] = value
    }
    
    public fun metadata(metadata: Map<String, String>) {
        globalMetadata.putAll(metadata)
    }
    
    public fun build(): PulseKitConfig = PulseKitConfig(
        apiKey = apiKey,
        baseUrl = baseUrl,
        enableDebugLogging = enableDebugLogging,
        maxQueueSize = maxQueueSize,
        flushInterval = flushInterval,
        maxEventAge = maxEventAge,
        sessionTimeout = sessionTimeout,
        enableAutoSessionManagement = enableAutoSessionManagement,
        enableOfflineQueueing = enableOfflineQueueing,
        enableDiskPersistence = enableDiskPersistence,
        maxDatabaseSize = maxDatabaseSize,
        databaseCleanupInterval = databaseCleanupInterval,
        userAgent = userAgent,
        globalMetadata = globalMetadata.toMap(),
        backpressureConfig = BackpressureConfig(
            maxInMemoryQueueSize = maxInMemoryQueueSize,
            maxDiskQueueSize = maxDiskQueueSize,
            dropPolicy = dropPolicy,
            enablePriorityDropping = enablePriorityDropping,
            backpressureThreshold = backpressureThreshold,
            dropWhenDiskFull = dropWhenDiskFull,
            maxEventRetries = maxEventRetries
        )
    )
}
