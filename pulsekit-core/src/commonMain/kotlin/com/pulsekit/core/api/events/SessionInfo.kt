package com.pulsekit.core.api.events

/**
 * Information about the current session.
 */
data class SessionInfo(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long?,
    val isActive: Boolean
)
