package com.pulsekit.core.api.status

import com.pulsekit.core.api.config.PulseKitConfig
import com.pulsekit.core.api.session.SessionInfo

/**
 * Status information for the PulseKit SDK.
 *
 * Useful for debugging, monitoring, and health checks.
 */
public data class PulseKitStatus(
    /**
     * Whether the SDK is properly initialized.
     */
    public val isInitialized: Boolean,

    /**
     * The current configuration.
     */
    public val config: PulseKitConfig,

    /**
     * Information about the current session, if any.
     */
    public val sessionInfo: SessionInfo?,

    /**
     * Number of events currently queued for processing.
     */
    public val queuedEventCount: Int,

    /**
     * Whether the SDK is currently processing events.
     */
    public val isProcessing: Boolean = false,

    /**
     * Last error that occurred, if any.
     */
    public val lastError: String? = null,

    /**
     * SDK version.
     */
    public val version: String = "0.1.0",

    /**
     * Timestamp when this status was generated.
     */
    public val timestamp: kotlinx.datetime.Instant = kotlinx.datetime.Clock.System.now(),

    /**
     * Currently active experimental feature flags.
     */
    public val activeFeatureFlags: List<String> = emptyList(),
)
