package com.pulsekit.core.api.errors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PulseKitErrorTest {

    @Test
    fun notInitialized_hasExpectedMessageAndCode() {
        val error = InitializationError.NotInitialized()
        assertNotNull(error.message)
        assertEquals(true, error.message!!.contains("not been initialized"))
        assertEquals("NOT_INITIALIZED", error.code)
    }

    @Test
    fun alreadyInitialized_hasExpectedMessageAndCode() {
        val error = InitializationError.AlreadyInitialized()
        assertNotNull(error.message)
        assertEquals(true, error.message!!.contains("already initialized"))
        assertEquals("ALREADY_INITIALIZED", error.code)
    }

    @Test
    fun eventError_queueFull_hasExpectedMessageAndCode() {
        val error = EventError.QueueFull(currentSize = 10, maxSize = 10)
        assertNotNull(error.message)
        assertEquals(true, error.message.contains("full"))
        assertEquals("QUEUE_FULL", error.code)
        assertEquals(10, error.currentSize)
        assertEquals(10, error.maxSize)
    }

    @Test
    fun pulseKitError_event_invalidEvent_hasMessage() {
        val error = PulseKitError.Event.InvalidEvent("event name cannot be blank")
        assertEquals("event name cannot be blank", error.message)
    }

    @Test
    fun pulseKitError_event_eventTooLarge_hasSizes() {
        val error = PulseKitError.Event.EventTooLarge(eventSize = 100, maxSize = 50)
        assertEquals(100, error.eventSize)
        assertEquals(50, error.maxSize)
        assertNotNull(error.message)
    }

    @Test
    fun eventError_invalidEvent_hasCode() {
        val error = EventError.InvalidEvent("invalid", null)
        assertEquals("INVALID_EVENT", error.code)
    }
}
