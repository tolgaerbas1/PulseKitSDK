package com.pulsekit.core.api.events

import kotlin.time.Duration.Companion.milliseconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PulseEventTest {

    @Test
    fun customEvent_hasCorrectEventName() {
        val event = CustomEvent("custom_action", emptyMap())
        assertEquals("custom_action", event.eventName)
        assertEquals(emptyMap<String, String>(), event.metadata)
        assertNotNull(event.eventId)
        assertNotNull(event.timestamp)
    }

    @Test
    fun customEvent_withMetadata() {
        val event = CustomEvent(
            eventName = "action",
            metadata = mapOf("k" to "v"),
            value = 42.0,
            category = "test"
        )
        assertEquals("action", event.eventName)
        assertEquals(mapOf("k" to "v"), event.metadata)
        assertEquals(42.0, event.value)
        assertEquals("test", event.category)
    }

    @Test
    fun engagementEvent_hasDerivedEventName() {
        val event = EngagementEvent(
            action = EngagementAction.CLICK,
            target = "btn",
            duration = 150.milliseconds
        )
        assertEquals("engagement_click", event.eventName)
        assertEquals(EngagementAction.CLICK, event.action)
        assertEquals("btn", event.target)
    }

    @Test
    fun performanceEvent_hasDerivedEventName() {
        val event = PerformanceEvent(
            metric = "latency",
            value = 16.7,
            unit = "ms"
        )
        assertEquals("performance_latency", event.eventName)
        assertEquals("latency", event.metric)
        assertEquals(16.7, event.value, 0.0)
        assertEquals("ms", event.unit)
    }

    @Test
    fun errorEvent_hasDerivedEventName() {
        val event = ErrorEvent(
            errorType = ErrorType.RUNTIME,
            message = "test error",
            isFatal = false
        )
        assertEquals("error_runtime", event.eventName)
        assertEquals(ErrorType.RUNTIME, event.errorType)
        assertEquals("test error", event.message)
    }

    @Test
    fun lifecycleEvent_hasDerivedEventName() {
        val event = LifecycleEvent(
            action = LifecycleAction.FOREGROUND,
            component = "MainActivity"
        )
        assertEquals("lifecycle_foreground", event.eventName)
        assertEquals(LifecycleAction.FOREGROUND, event.action)
        assertEquals("MainActivity", event.component)
    }
}
