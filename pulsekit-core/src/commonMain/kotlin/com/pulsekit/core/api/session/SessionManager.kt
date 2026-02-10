package com.pulsekit.core.api.session

import com.pulsekit.core.api.config.PulseKitConfig
import com.pulsekit.core.api.events.LifecycleAction
import com.pulsekit.core.api.events.LifecycleEvent
import com.pulsekit.core.api.events.PulseEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages user sessions with automatic timeout handling.
 * 
 * Sessions are automatically started and ended based on activity
 * and configured timeout values.
 */
public class SessionManager(
    private val config: PulseKitConfig,
    private val scope: CoroutineScope
) {

    private var currentSession: Session? = null
    private var timeoutJob: Job? = null
    private var sessionListener: SessionListener? = null
    private var onTrackEvent: ((PulseEvent) -> Unit)? = null
    
    /**
     * Start a new session.
     * 
     * If a session is already active, it will be ended first.
     */
    public fun startNewSession() {
        endCurrentSession()
        
        val sessionId = SessionId.generate()
        val session = Session(
            id = sessionId,
            startTime = kotlinx.datetime.Clock.System.now(),
            metadata = config.globalMetadata
        )
        
        currentSession = session
        startTimeoutTimer()
        
        sessionListener?.onSessionStarted(
            com.pulsekit.core.api.events.SessionInfo(
                sessionId = session.id.value,
                startTime = session.startTime.toEpochMilliseconds(),
                endTime = session.endTime?.toEpochMilliseconds(),
                isActive = session.endTime == null
            )
        )
        onTrackEvent?.invoke(
            LifecycleEvent(
                action = LifecycleAction.START,
                component = "session",
                metadata = mapOf("session_id" to session.id.value)
            )
        )
    }
    
    /**
     * End the current session if one exists.
     */
    public fun endCurrentSession() {
        currentSession?.let { session ->
            cancelTimeoutTimer()
            
            val endTime = kotlinx.datetime.Clock.System.now()
            val completedSession = session.copy(
                endTime = endTime,
                duration = endTime - session.startTime
            )
            
            sessionListener?.onSessionEnded(
                com.pulsekit.core.api.events.SessionInfo(
                    sessionId = completedSession.id.value,
                    startTime = completedSession.startTime.toEpochMilliseconds(),
                    endTime = completedSession.endTime?.toEpochMilliseconds(),
                    isActive = completedSession.endTime == null
                )
            )
            onTrackEvent?.invoke(
                LifecycleEvent(
                    action = LifecycleAction.STOP,
                    component = "session",
                    metadata = mapOf("session_id" to completedSession.id.value)
                )
            )
            currentSession = null
        }
    }
    
    /**
     * Get information about the current session.
     */
    public fun getCurrentSessionInfo(): com.pulsekit.core.api.events.SessionInfo? {
        return currentSession?.let { session ->
            com.pulsekit.core.api.events.SessionInfo(
                sessionId = session.id.value,
                startTime = session.startTime.toEpochMilliseconds(),
                endTime = session.endTime?.toEpochMilliseconds(),
                isActive = session.endTime == null
            )
        }
    }
    
    /**
     * Refresh the session timeout.
     * 
     * Call this when user activity is detected to prevent session timeout.
     */
    public fun refreshSession() {
        if (currentSession != null) {
            cancelTimeoutTimer()
            startTimeoutTimer()
        }
    }
    
    /**
     * Set a listener for session events.
     */
    public fun setSessionListener(listener: SessionListener?) {
        sessionListener = listener
    }

    /**
     * Set callback to track session start/end as LifecycleEvents.
     * Called with LifecycleEvent(START) on session start and LifecycleEvent(STOP) on session end.
     */
    internal fun setOnTrackEvent(callback: ((PulseEvent) -> Unit)?) {
        onTrackEvent = callback
    }
    
    /**
     * Clean up resources.
     */
    internal fun cleanup() {
        endCurrentSession()
        sessionListener = null
    }
    
    private fun startTimeoutTimer() {
        if (config.sessionTimeout.isPositive()) {
            timeoutJob = scope.launch {
                delay(config.sessionTimeout)
                if (isActive && currentSession != null) {
                    // Session timed out
                    sessionListener?.onSessionTimedOut(
                        com.pulsekit.core.api.events.SessionInfo(
                            sessionId = currentSession!!.id.value,
                            startTime = currentSession!!.startTime.toEpochMilliseconds(),
                            endTime = currentSession!!.endTime?.toEpochMilliseconds(),
                            isActive = currentSession!!.endTime == null
                        )
                    )
                    endCurrentSession()
                }
            }
        }
    }
    
    private fun cancelTimeoutTimer() {
        timeoutJob?.cancel()
        timeoutJob = null
    }
}

/**
 * Internal session data structure.
 */
data class Session(
    val id: SessionId,
    val startTime: kotlinx.datetime.Instant,
    val endTime: kotlinx.datetime.Instant? = null,
    val duration: kotlin.time.Duration? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Public information about a session.
 */
public data class SessionInfo(
    public val sessionId: SessionId,
    public val startTime: kotlinx.datetime.Instant,
    public val endTime: kotlinx.datetime.Instant?,
    public val duration: kotlin.time.Duration,
    public val isActive: Boolean,
    public val metadata: Map<String, String>
)

/**
 * Unique identifier for sessions.
 */
@JvmInline
public value class SessionId(public val value: String) {
    
    public companion object {
        public fun generate(): SessionId = SessionId(
            "sess_${kotlinx.datetime.Clock.System.now().epochSeconds}_${(0..999).random()}"
        )
    }
}

/**
 * Listener for session lifecycle events.
 */
public interface SessionListener {
    
    /**
     * Called when a new session starts.
     */
    public fun onSessionStarted(session: com.pulsekit.core.api.events.SessionInfo) {}
    
    /**
     * Called when a session ends normally.
     */
    public fun onSessionEnded(session: com.pulsekit.core.api.events.SessionInfo) {}
    
    /**
     * Called when a session times out due to inactivity.
     */
    public fun onSessionTimedOut(session: com.pulsekit.core.api.events.SessionInfo) {}
}
