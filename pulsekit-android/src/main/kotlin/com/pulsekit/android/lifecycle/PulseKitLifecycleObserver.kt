package com.pulsekit.android.lifecycle

import android.app.Application
import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.Lifecycle
import com.pulsekit.core.api.PulseKitInstance
import com.pulsekit.core.api.events.LifecycleEvent
import com.pulsekit.core.api.events.LifecycleAction
import com.pulsekit.core.api.events.SessionEvent
import com.pulsekit.core.api.events.SessionAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Advanced lifecycle observer that provides automatic session management
 * and comprehensive lifecycle tracking with zero integration work.
 * 
 * This class automatically:
 * - Tracks app foreground/background transitions
 * - Manages session lifecycle based on app state
 * - Handles session timeouts during inactivity
 * - Tracks detailed lifecycle events
 * - Provides session state callbacks
 */
public class PulseKitLifecycleObserver private constructor(
    private val application: Application,
    private val pulseKitInstance: PulseKitInstance
) : LifecycleObserver {
    
    private val lifecycleScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isForeground: Boolean = false
    private var sessionTimeoutJob: kotlinx.coroutines.Job? = null
    private var lastActivityTime: Long = System.currentTimeMillis()
    
    // Session state tracking
    private var currentSessionId: String? = null
    private var sessionStartTime: Long? = null
    private var sessionListener: SessionLifecycleListener? = null
    
    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        startSessionTimeoutMonitoring()
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public fun onAppForeground() {
        if (!isForeground) {
            isForeground = true
            lastActivityTime = System.currentTimeMillis()
            
            // Track detailed app foreground event
            pulseKitInstance.track(
                LifecycleEvent(
                    action = LifecycleAction.FOREGROUND,
                    component = "application",
                    metadata = mapOf(
                        "previous_state" to "background",
                        "timestamp" to System.currentTimeMillis().toString(),
                        "background_duration" to (System.currentTimeMillis() - (sessionStartTime ?: 0)).toString(),
                        "session_auto_resumed" to (currentSessionId != null).toString()
                    )
                )
            )
            
            // Resume or start session
            if (currentSessionId == null) {
                startNewSession()
            } else {
                resumeSession()
            }
            
            // Notify listener
            sessionListener?.onAppForeground()
        }
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public fun onAppBackground() {
        if (isForeground) {
            isForeground = false
            
            // Track detailed app background event
            val backgroundTime = System.currentTimeMillis()
            pulseKitInstance.track(
                LifecycleEvent(
                    action = LifecycleAction.BACKGROUND,
                    component = "application",
                    metadata = mapOf(
                        "previous_state" to "foreground",
                        "timestamp" to backgroundTime.toString(),
                        "session_duration" to sessionStartTime?.let { backgroundTime - it }.toString(),
                        "session_id" to (currentSessionId ?: "none")
                    )
                )
            )
            
            // Pause session (don't end immediately - allow for quick returns)
            pauseSession()
            
            // Notify listener
            sessionListener?.onAppBackground()
        }
    }
    
    /**
     * Start a new session with comprehensive tracking.
     */
    private fun startNewSession() {
        val currentTime = System.currentTimeMillis()
        sessionStartTime = currentTime
        currentSessionId = "sess_${currentTime}_${(0..999).random()}"
        
        // Track session start
        pulseKitInstance.track(
            SessionEvent(
                action = SessionAction.START,
                sessionId = com.pulsekit.core.api.session.SessionId(currentSessionId!!),
                metadata = mapOf(
                    "trigger" to "lifecycle_foreground",
                    "auto_managed" to "true",
                    "timestamp" to currentTime.toString()
                )
            )
        )
        
        // Start SDK session
        pulseKitInstance.startSession()
        
        // Start session timeout monitoring
        startSessionTimeoutMonitoring()
        
        // Notify listener
        sessionListener?.onSessionStarted(currentSessionId!!)
    }
    
    /**
     * Resume an existing session.
     */
    private fun resumeSession() {
        val currentTime = System.currentTimeMillis()
        
        // Track session resume
        pulseKitInstance.track(
            SessionEvent(
                action = SessionAction.RESUME,
                sessionId = com.pulsekit.core.api.session.SessionId(currentSessionId!!),
                metadata = mapOf(
                    "trigger" to "lifecycle_foreground",
                    "background_duration" to (currentTime - lastActivityTime).toString(),
                    "timestamp" to currentTime.toString()
                )
            )
        )
        
        // Resume SDK session
        pulseKitInstance.startSession()
        
        // Restart timeout monitoring
        startSessionTimeoutMonitoring()
        
        // Notify listener
        sessionListener?.onSessionResumed(currentSessionId!!)
    }
    
    /**
     * Pause the current session (don't end immediately).
     */
    private fun pauseSession() {
        // Cancel timeout monitoring
        sessionTimeoutJob?.cancel()
        
        // Notify listener
        currentSessionId?.let { sessionId ->
            sessionListener?.onSessionPaused(sessionId)
        }
    }
    
    /**
     * End the current session completely.
     */
    private fun endSession(reason: String = "lifecycle_background") {
        currentSessionId?.let { sessionId ->
            val currentTime = System.currentTimeMillis()
            
            // Track session end
            pulseKitInstance.track(
                SessionEvent(
                    action = SessionAction.END,
                    sessionId = com.pulsekit.core.api.session.SessionId(sessionId),
                    metadata = mapOf(
                        "reason" to reason,
                        "session_duration" to sessionStartTime?.let { currentTime - it }.toString(),
                        "timestamp" to currentTime.toString()
                    )
                )
            )
            
            // End SDK session
            pulseKitInstance.endSession()
            
            // Notify listener
            sessionListener?.onSessionEnded(sessionId)
            
            // Clear session state
            currentSessionId = null
            sessionStartTime = null
        }
        
        // Cancel timeout monitoring
        sessionTimeoutJob?.cancel()
    }
    
    /**
     * Start monitoring for session timeout during inactivity.
     */
    private fun startSessionTimeoutMonitoring() {
        sessionTimeoutJob?.cancel()
        
        if (currentSessionId != null && isForeground) {
            sessionTimeoutJob = lifecycleScope.launch {
                while (isActive && isForeground) {
                    delay(30.seconds) // Check every 30 seconds
                    
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastActivity = currentTime - lastActivityTime
                    
                    // Check if session should timeout (5 minutes of inactivity)
                    if (timeSinceLastActivity > 5 * 60 * 1000) {
                        handleSessionTimeout()
                        break
                    }
                }
            }
        }
    }
    
    /**
     * Handle session timeout due to inactivity.
     */
    private fun handleSessionTimeout() {
        currentSessionId?.let { sessionId ->
            val currentTime = System.currentTimeMillis()
            
            // Track session timeout
            pulseKitInstance.track(
                SessionEvent(
                    action = SessionAction.TIMEOUT,
                    sessionId = com.pulsekit.core.api.session.SessionId(sessionId),
                    metadata = mapOf(
                        "timeout_duration" to (currentTime - lastActivityTime).toString(),
                        "timestamp" to currentTime.toString()
                    )
                )
            )
            
            // End session
            endSession("inactivity_timeout")
            
            // Notify listener
            sessionListener?.onSessionTimedOut(sessionId)
        }
    }
    
    /**
     * Update activity timestamp to prevent session timeout.
     * Call this when user activity is detected.
     */
    public fun updateActivity() {
        lastActivityTime = System.currentTimeMillis()
        
        // Restart timeout monitoring
        if (isForeground && currentSessionId != null) {
            startSessionTimeoutMonitoring()
        }
    }
    
    /**
     * Set a listener for detailed session lifecycle events.
     */
    public fun setSessionListener(listener: SessionLifecycleListener?) {
        sessionListener = listener
    }
    
    /**
     * Get current session information.
     */
    public fun getCurrentSessionInfo(): SessionInfo? {
        return currentSessionId?.let { sessionId ->
            SessionInfo(
                sessionId = sessionId,
                startTime = sessionStartTime ?: 0,
                isForeground = isForeground,
                isPaused = !isForeground && currentSessionId != null,
                lastActivityTime = lastActivityTime
            )
        }
    }
    
    public companion object {
        private var instance: PulseKitLifecycleObserver? = null
        
        /**
         * Initialize the lifecycle observer with automatic session management.
         * 
         * This method provides zero integration work - just call it once during
         * SDK initialization and everything else is handled automatically.
         * 
         * @param context Application context
         * @param pulseKitInstance The PulseKit instance to integrate with
         */
        public fun initialize(
            context: Context,
            pulseKitInstance: PulseKitInstance
        ) {
            if (instance == null) {
                val application = context.applicationContext as Application
                instance = PulseKitLifecycleObserver(application, pulseKitInstance)
            }
        }
        
        /**
         * Get the application context if initialized.
         */
        public fun getContext(): Context? {
            return instance?.application
        }
        
        /**
         * Check if the app is currently in foreground.
         */
        public fun isAppInForeground(): Boolean {
            return instance?.isForeground ?: false
        }
        
        /**
         * Get current session information.
         */
        public fun getCurrentSessionInfo(): SessionInfo? {
            return instance?.getCurrentSessionInfo()
        }
        
        /**
         * Update activity timestamp to prevent session timeout.
         * Call this when user activity is detected (button clicks, etc.).
         */
        public fun updateActivity() {
            instance?.updateActivity()
        }
        
        /**
         * Set a listener for detailed session lifecycle events.
         */
        public fun setSessionListener(listener: SessionLifecycleListener?) {
            instance?.setSessionListener(listener)
        }
        
        /**
         * Cleanup the lifecycle observer.
         */
        public fun cleanup() {
            instance?.let { observer ->
                observer.endSession("cleanup")
                ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
                observer.lifecycleScope.cancel()
                instance = null
            }
        }
    }
}

/**
 * Detailed session information.
 */
public data class SessionInfo(
    public val sessionId: String,
    public val startTime: Long,
    public val isForeground: Boolean,
    public val isPaused: Boolean,
    public val lastActivityTime: Long
)

/**
 * Listener for detailed session lifecycle events.
 */
public interface SessionLifecycleListener {
    
    /**
     * Called when the app comes to foreground.
     */
    public fun onAppForeground() {}
    
    /**
     * Called when the app goes to background.
     */
    public fun onAppBackground() {}
    
    /**
     * Called when a new session starts.
     */
    public fun onSessionStarted(sessionId: String) {}
    
    /**
     * Called when a session is resumed (app returns to foreground).
     */
    public fun onSessionResumed(sessionId: String) {}
    
    /**
     * Called when a session is paused (app goes to background).
     */
    public fun onSessionPaused(sessionId: String) {}
    
    /**
     * Called when a session ends normally.
     */
    public fun onSessionEnded(sessionId: String) {}
    
    /**
     * Called when a session times out due to inactivity.
     */
    public fun onSessionTimedOut(sessionId: String) {}
}
