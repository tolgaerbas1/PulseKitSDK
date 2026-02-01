package com.pulsekit.core.api.session

import com.pulsekit.core.api.config.PulseKitConfig
import com.pulsekit.core.api.events.SessionEvent
import com.pulsekit.core.api.events.SessionAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration

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
        
        sessionListener?.onSessionStarted(session)
        
        // TODO: Track session start event
        // eventProcessor.process(SessionEvent(SessionAction.START, sessionId))
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
            
            sessionListener?.onSessionEnded(completedSession)
            
            // TODO: Track session end event
            // eventProcessor.process(SessionEvent(SessionAction.END, session.id))
            
            currentSession = null
        }
    }
    
    /**
     * Get information about the current session.
     */
    public fun getCurrentSession(): SessionInfo? {
        return currentSession?.let { session ->
            val now = kotlinx.datetime.Clock.System.now()
            SessionInfo(
                sessionId = session.id,
                startTime = session.startTime,
                endTime = session.endTime,
                duration = session.endTime?.let { it - session.startTime } ?: now - session.startTime,
                isActive = session.endTime == null,
                metadata = session.metadata
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
                    sessionListener?.onSessionTimedOut(currentSession!!)
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
private data class Session(
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
    public fun onSessionStarted(session: SessionInfo) {}
    
    /**
     * Called when a session ends normally.
     */
    public fun onSessionEnded(session: SessionInfo) {}
    
    /**
     * Called when a session times out due to inactivity.
     */
    public fun onSessionTimedOut(session: SessionInfo) {}
}
