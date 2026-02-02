package com.pulsekit.core.api.events

import com.pulsekit.core.api.session.SessionId
import kotlinx.datetime.Instant

/**
 * Base class for all PulseKit events.
 * 
 * All events must extend this sealed class to ensure type safety
 * and prevent arbitrary event types from being tracked.
 */
public sealed class PulseEvent {
    
    /**
     * Unique identifier for this event instance.
     * Generated automatically when the event is created.
     */
    public val eventId: EventId = EventId.generate()
    
    /**
     * Timestamp when the event was created.
     */
    public val timestamp: Instant = kotlinx.datetime.Clock.System.now()
    
    /**
     * Optional name for grouping related events.
     * Useful for analytics and filtering.
     */
    public abstract val eventName: String
    
    /**
     * Optional metadata associated with this event.
     * Can be used to add context-specific information.
     */
    public abstract val metadata: Map<String, String>
}

/**
 * Custom event for tracking arbitrary user actions.
 * 
 * Use this for application-specific events that don't fit
 * into the predefined event types.
 */
public class CustomEvent(
    override val eventName: String,
    override val metadata: Map<String, String> = emptyMap(),
    public val value: Double? = null,
    public val category: String? = null
) : PulseEvent()

/**
 * Event tracking user engagement and interactions.
 */
public class EngagementEvent(
    public val action: EngagementAction,
    public val target: String? = null,
    public val duration: kotlin.time.Duration? = null,
    override val metadata: Map<String, String> = emptyMap()
) : PulseEvent() {
    
    override val eventName: String = "engagement_${action.name.lowercase()}"
}

/**
 * Event tracking application lifecycle events.
 */
public class LifecycleEvent(
    public val action: LifecycleAction,
    public val component: String,
    override val metadata: Map<String, String> = emptyMap()
) : PulseEvent() {
    
    override val eventName: String = "lifecycle_${action.name.lowercase()}"
}

/**
 * Event tracking performance metrics.
 */
public class PerformanceEvent(
    public val metric: String,
    public val value: Double,
    public val unit: String,
    override val metadata: Map<String, String> = emptyMap()
) : PulseEvent() {
    
    override val eventName: String = "performance_$metric"
}

/**
 * Event tracking errors and exceptions.
 */
public class ErrorEvent(
    public val errorType: ErrorType,
    public val message: String,
    public val stackTrace: String? = null,
    public val isFatal: Boolean = false,
    override val metadata: Map<String, String> = emptyMap()
) : PulseEvent() {
    
    override val eventName: String = "error_${errorType.name.lowercase()}"
}

/**
 * Event tracking user session information.
 */
public class SessionEvent(
    public val action: SessionAction,
    public val sessionId: SessionId,
    override val metadata: Map<String, String> = emptyMap()
) : PulseEvent() {
    
    override val eventName: String = "session_${action.name.lowercase()}"
}

/**
 * Types of engagement actions.
 */
public enum class EngagementAction {
    CLICK,
    VIEW,
    SWIPE,
    SCROLL,
    TAP,
    LONG_PRESS,
    FOCUS,
    BLUR,
    SELECT,
    DESELECT,
    ERROR,
    CRASH,
    SESSION_START,
    SESSION_END
}

/**
 * Types of lifecycle actions.
 */
public enum class LifecycleAction {
    START,
    STOP,
    RESUME,
    PAUSE,
    CREATE,
    DESTROY,
    FOREGROUND,
    BACKGROUND
}

/**
 * Types of errors.
 */
public enum class ErrorType {
    RUNTIME,
    NETWORK,
    MEMORY,
    PERMISSION,
    CONFIGURATION,
    UNKNOWN
}

/**
 * Types of session actions.
 */
public enum class SessionAction {
    START,
    END,
    TIMEOUT,
    CRASH,
    RESUME
}

/**
 * Create a copy of a PulseEvent with updated metadata.
 */
public fun PulseEvent.withMetadata(metadata: Map<String, String>): PulseEvent = when (this) {
    is CustomEvent -> CustomEvent(
        eventName = eventName,
        metadata = metadata,
        value = value,
        category = category
    )
    is EngagementEvent -> EngagementEvent(
        action = action,
        target = target,
        duration = duration,
        metadata = metadata
    )
    is PerformanceEvent -> PerformanceEvent(
        metric = metric,
        value = value,
        unit = unit,
        metadata = metadata
    )
    is ErrorEvent -> ErrorEvent(
        errorType = errorType,
        message = message,
        stackTrace = stackTrace,
        isFatal = isFatal,
        metadata = metadata
    )
    is LifecycleEvent -> LifecycleEvent(
        action = action,
        component = component,
        metadata = metadata
    )
    is SessionEvent -> SessionEvent(
        action = action,
        sessionId = sessionId,
        metadata = metadata
    )
}

/**
 * Unique identifier for events.
 */
@JvmInline
public value class EventId(public val value: String) {
    
    public companion object {
        public fun generate(): EventId = EventId(
            "evt_${kotlinx.datetime.Clock.System.now().epochSeconds}_${(0..999).random()}"
        )
    }
}
