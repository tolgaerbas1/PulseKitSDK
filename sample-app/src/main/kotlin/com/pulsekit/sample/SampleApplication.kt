package com.pulsekit.sample

import android.app.Application
import com.pulsekit.android.PulseKitAndroid
import com.pulsekit.android.lifecycle.SessionLifecycleListener
import com.pulsekit.core.api.config.PulseKitConfig
import kotlin.time.Duration.Companion.minutes

/**
 * Sample application demonstrating PulseKit initialization with zero integration work.
 * 
 * This shows how simple it is to get started with PulseKit - just one line of code
 * in your Application class and everything else is handled automatically:
 * 
 * - Session management based on app lifecycle
 * - Automatic event tracking for foreground/background transitions
 * - Session timeout handling
 * - Offline-first event persistence
 * - Activity monitoring
 */
class SampleApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize PulseKit with custom configuration
        val config = PulseKitConfig {
            apiKey = "demo-api-key" // Replace with your actual API key
            enableDebugLogging = true
            enableAutoSessionManagement = true
            enableOfflineQueueing = true
            enableDiskPersistence = true
            maxQueueSize = 500
            flushInterval = 2.minutes
            sessionTimeout = 15.minutes
            
            // Add global metadata
            metadata("app_version", "1.0.0")
            metadata("build_type", "debug")
            metadata("sample_app", "true")
        }
        
        // One line initialization - everything else is automatic!
        PulseKitAndroid.initialize(this, config)
        
        // Optional: Set up session lifecycle listener for debugging
        PulseKitAndroid.setSessionListener(object : SessionLifecycleListener {
            override fun onSessionStarted(sessionId: String) {
                println("🟢 Session started: $sessionId")
            }
            
            override fun onSessionResumed(sessionId: String) {
                println("🔄 Session resumed: $sessionId")
            }
            
            override fun onSessionPaused(sessionId: String) {
                println("⏸️ Session paused: $sessionId")
            }
            
            override fun onSessionEnded(sessionId: String) {
                println("🔴 Session ended: $sessionId")
            }
            
            override fun onSessionTimedOut(sessionId: String) {
                println("⏰ Session timed out: $sessionId")
            }
            
            override fun onAppForeground() {
                println("☀️ App came to foreground")
            }
            
            override fun onAppBackground() {
                println("🌙 App went to background")
            }
        })
    }
}
