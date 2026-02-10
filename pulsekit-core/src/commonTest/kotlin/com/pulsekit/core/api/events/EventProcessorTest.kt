package com.pulsekit.core.api.events

import com.pulsekit.core.api.config.PulseKitConfig
import com.pulsekit.core.api.errors.PulseKitError
import com.pulsekit.core.api.flags.FeatureFlag
import com.pulsekit.core.api.storage.EventQueue
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test implementation of FlagProvider that returns fixed values.
 */
private class TestFlagProvider(
    private val booleanValue: Boolean = true,
    private val longValue: Long = 50L,
    private val doubleValue: Double = 0.0,
    private val stringValue: String = ""
) : FlagProvider {
    override fun getBooleanFlag(flag: FeatureFlag): Boolean = booleanValue
    override fun getIntegerFlag(flag: FeatureFlag): Long = longValue
    override fun getDoubleFlag(flag: FeatureFlag): Double = doubleValue
    override fun getStringFlag(flag: FeatureFlag): String = stringValue
}

class EventProcessorTest {

    private fun createProcessor(
        config: PulseKitConfig = PulseKitConfig(),
        flagProvider: FlagProvider = TestFlagProvider()
    ): Pair<EventProcessor, EventQueue> {
        val scope = TestScope(StandardTestDispatcher())
        val queue = EventQueue(config, scope)
        val processor = EventProcessor.create(config, queue, scope, flagProvider)
        return processor to queue
    }

    @Test
    fun process_blankEventName_throwsInvalidEvent() = runTest {
        val (processor, _) = createProcessor()
        try {
            processor.process(CustomEvent("", emptyMap()))
            throw AssertionError("Expected PulseKitError.Event.InvalidEvent")
        } catch (e: PulseKitError.Event.InvalidEvent) {
            assertTrue(e.message!!.contains("blank") || e.message.isNotBlank())
        }
    }

    @Test
    fun process_validEvent_enqueuesToQueue() = runTest {
        val (processor, queue) = createProcessor()
        processor.process(CustomEvent("valid_event", mapOf("k" to "v")))
        assertTrue(queue.size() >= 1)
        val batch = queue.getNextBatch(10)
        assertTrue(batch.any { it.eventName == "valid_event" })
    }

    @Test
    fun process_validEvent_doesNotThrow() = runTest {
        val (processor, _) = createProcessor()
        processor.process(CustomEvent("test", mapOf("a" to "b")))
    }

    @Test
    fun process_metadataTooLongValue_throwsInvalidEvent() = runTest {
        val (processor, _) = createProcessor()
        val longValue = "x".repeat(1025)
        try {
            processor.process(CustomEvent("event", mapOf("key" to longValue)))
            throw AssertionError("Expected InvalidEvent")
        } catch (e: PulseKitError.Event.InvalidEvent) {
            assertTrue(e.message!!.contains("too long") || e.message.isNotBlank())
        }
    }
}
