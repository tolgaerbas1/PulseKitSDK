package com.pulsekit.sample

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pulsekit.android.PulseKitAndroid
import com.pulsekit.core.api.events.CustomEvent
import com.pulsekit.core.api.events.EngagementEvent
import com.pulsekit.core.api.events.EngagementAction
import com.pulsekit.core.api.events.PerformanceEvent
import com.pulsekit.core.api.events.ErrorEvent
import com.pulsekit.core.api.events.ErrorType
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main activity demonstrating PulseKit event tracking.
 */
class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupEventTracking()
    }
    
    private fun setupEventTracking() {
        // Track button click events with automatic activity monitoring
        findViewById<Button>(R.id.btn_track_custom).setOnClickListener {
            // Update activity to prevent session timeout
            PulseKitAndroid.updateActivity()
            trackCustomEvent()
        }
        
        findViewById<Button>(R.id.btn_track_engagement).setOnClickListener {
            PulseKitAndroid.updateActivity()
            trackEngagementEvent()
        }
        
        findViewById<Button>(R.id.btn_track_performance).setOnClickListener {
            PulseKitAndroid.updateActivity()
            trackPerformanceEvent()
        }
        
        findViewById<Button>(R.id.btn_track_error).setOnClickListener {
            PulseKitAndroid.updateActivity()
            trackErrorEvent()
        }
        
        findViewById<Button>(R.id.btn_session_info).setOnClickListener {
            PulseKitAndroid.updateActivity()
            showSessionInfo()
        }
        
        findViewById<Button>(R.id.btn_flush_events).setOnClickListener {
            PulseKitAndroid.updateActivity()
            flushEvents()
        }
        
        // Add backpressure demo button
        findViewById<Button>(R.id.btn_backpressure_demo).setOnClickListener {
            PulseKitAndroid.updateActivity()
            backpressureDemo()
        }
        
        // Track app screen view
        PulseKitAndroid.instance.track(
            CustomEvent(
                eventName = "screen_view",
                metadata = mapOf(
                    "screen_name" to "MainActivity",
                    "screen_class" to this::class.java.simpleName
                )
            )
        )
    }
    
    private fun trackCustomEvent() {
        PulseKitAndroid.instance.track(
            CustomEvent(
                eventName = "demo_action",
                category = "demo",
                value = 42.0,
                metadata = mapOf(
                    "button_id" to "btn_track_custom",
                    "timestamp" to System.currentTimeMillis().toString(),
                    "user_action" to "custom_event_demo"
                )
            )
        )
        
        showToast("Custom event tracked")
    }
    
    private fun trackEngagementEvent() {
        PulseKitAndroid.instance.track(
            EngagementEvent(
                action = EngagementAction.CLICK,
                target = "demo_button",
                duration = 150.milliseconds,
                metadata = mapOf(
                    "button_text" to "Track Engagement",
                    "coordinates" to "100,200"
                )
            )
        )
        
        showToast("Engagement event tracked")
    }
    
    private fun trackPerformanceEvent() {
        val startTime = System.currentTimeMillis()
        
        // Simulate some work
        Thread {
            try {
                Thread.sleep(100)
                val duration = System.currentTimeMillis() - startTime
                
                PulseKitAndroid.instance.track(
                    PerformanceEvent(
                        metric = "button_click_duration",
                        value = duration.toDouble(),
                        unit = "ms",
                        metadata = mapOf(
                            "button_type" to "performance_demo",
                            "thread_name" to Thread.currentThread().name
                        )
                    )
                )
                
                runOnUiThread { showToast("Performance event tracked (${duration}ms)") }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }.start()
    }
    
    private fun trackErrorEvent() {
        try {
            // Simulate an error
            throw RuntimeException("Demo error for testing")
        } catch (e: Exception) {
            PulseKitAndroid.instance.track(
                ErrorEvent(
                    errorType = ErrorType.RUNTIME,
                    message = e.message ?: "Unknown error",
                    stackTrace = e.stackTraceToString(),
                    isFatal = false,
                    metadata = mapOf(
                        "error_source" to "demo_button",
                        "user_triggered" to "true"
                    )
                )
            )
            
            showToast("Error event tracked")
        }
    }
    
    private fun showSessionInfo() {
        val sessionInfo = PulseKitAndroid.instance.getCurrentSession()
        val androidSessionInfo = PulseKitAndroid.getCurrentSessionInfo()
        val status = PulseKitAndroid.instance.getStatus()
        
        val message = if (sessionInfo != null && androidSessionInfo != null) {
            """
            Session Active: ${sessionInfo.isActive}
            Session ID: ${sessionInfo.sessionId}
            Start Time: ${sessionInfo.startTime}
            End Time: ${sessionInfo.endTime ?: "-"}
            App Foreground: ${PulseKitAndroid.isAppInForeground()}
            Session Paused: ${androidSessionInfo.isPaused}
            Last Activity: ${androidSessionInfo.lastActivityTime}
            SDK Status: $status
            """.trimIndent()
        } else {
            """
            App Foreground: ${PulseKitAndroid.isAppInForeground()}
            No active session
            """.trimIndent()
        }
        
        showToast(message)
    }
    
    private fun flushEvents() {
        PulseKitAndroid.instance.flush()
        showToast("Events flushed")
    }
    
    private fun backpressureDemo() {
        // Generate high volume of events to demonstrate backpressure
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            repeat(1500) { i ->
                val event = when (i % 4) {
                    0 -> CustomEvent("backpressure_test_low", metadata = mapOf("priority" to "low"))
                    1 -> CustomEvent("backpressure_test_medium", metadata = mapOf("priority" to "medium"))
                    2 -> PerformanceEvent("backpressure_test_high", 100.0, "ms")
                    else -> ErrorEvent(com.pulsekit.core.api.events.ErrorType.RUNTIME, "Backpressure test error", "", false)
                }
                
                PulseKitAndroid.instance.track(event)
                
                // Small delay to simulate real usage
                kotlinx.coroutines.delay(1)
            }
            
            // Show queue statistics
            val status = PulseKitAndroid.instance.getStatus()
            val message = """
            Backpressure Demo Complete!
            Generated 1500 events
            SDK Status: $status
            """.trimIndent()
            
            runOnUiThread {
                showToast(message)
            }
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
