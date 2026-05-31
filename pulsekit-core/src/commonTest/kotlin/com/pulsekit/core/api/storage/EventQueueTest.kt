package com.pulsekit.core.api.storage

import com.pulsekit.core.api.config.PulseKitConfig
import com.pulsekit.core.api.events.CustomEvent
import com.pulsekit.core.api.networking.EventBatchSender
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mock EventBatchSender that records sent payloads and returns configurable success.
 */
private class MockEventBatchSender(
    private val returnSuccess: Boolean = true,
) : EventBatchSender {
    val sentPayloads = mutableListOf<String>()

    override suspend fun sendBatch(jsonPayload: String): Boolean {
        sentPayloads.add(jsonPayload)
        return returnSuccess
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EventQueueTest {

    private fun createQueue(
        config: PulseKitConfig = PulseKitConfig(),
        batchSender: EventBatchSender? = null,
    ): Pair<EventQueue, TestScope> {
        val scope = TestScope(StandardTestDispatcher())
        val queue = EventQueue(config, scope, batchSender)
        return queue to scope
    }

    @Test
    fun enqueue_increasesSize() = runTest {
        val (queue, _) = createQueue()
        queue.enqueue(CustomEvent("e", emptyMap()))
        assertEquals(1, queue.size())
    }

    @Test
    fun getNextBatch_returnsEnqueuedEvents() = runTest {
        val (queue, _) = createQueue()
        val event = CustomEvent("batch_test", emptyMap())
        queue.enqueue(event)
        val batch = queue.getNextBatch(10)
        assertEquals(1, batch.size)
        assertEquals("batch_test", batch[0].eventName)
    }

    @Test
    fun markProcessed_removesEventsFromQueue() = runTest {
        val (queue, _) = createQueue()
        queue.enqueue(CustomEvent("a", emptyMap()))
        val batch = queue.getNextBatch(10)
        assertEquals(1, batch.size)
        queue.markProcessed(batch)
        assertEquals(0, queue.size())
    }

    @Test
    fun multipleEnqueues_andMarkProcessed() = runTest {
        val (queue, _) = createQueue()
        queue.enqueue(CustomEvent("e1", emptyMap()))
        queue.enqueue(CustomEvent("e2", emptyMap()))
        assertEquals(2, queue.size())
        val batch = queue.getNextBatch(10)
        assertEquals(2, batch.size)
        queue.markProcessed(batch)
        assertEquals(0, queue.size())
    }

    @Test
    fun backpressure_withSmallMaxSize_dropsOrCapsSize() = runTest {
        val config = PulseKitConfig(
            backpressureConfig = com.pulsekit.core.api.backpressure.BackpressureConfig(
                maxInMemoryQueueSize = 2,
                maxDiskQueueSize = 10,
                dropPolicy = com.pulsekit.core.api.backpressure.DropPolicy.DROP_OLDEST,
                enablePriorityDropping = true,
                backpressureThreshold = 0.9,
                dropWhenDiskFull = true,
            ),
        )
        val (queue, _) = createQueue(config)
        queue.enqueue(CustomEvent("e1", emptyMap()))
        queue.enqueue(CustomEvent("e2", emptyMap()))
        queue.enqueue(CustomEvent("e3", emptyMap()))
        assertTrue(queue.size() <= 2)
    }

    @Test
    fun isEmpty_initiallyTrue() = runTest {
        val (queue, _) = createQueue()
        assertTrue(queue.isEmpty())
    }

    @Test
    fun isEmpty_afterEnqueue_false() = runTest {
        val (queue, _) = createQueue()
        queue.enqueue(CustomEvent("e", emptyMap()))
        assertFalse(queue.isEmpty())
    }

    @Test
    fun flush_withBatchSender_success_removesEvents() = runTest {
        val mockSender = MockEventBatchSender(returnSuccess = true)
        val (queue, scope) = createQueue(batchSender = mockSender)
        queue.enqueue(CustomEvent("flush_test", emptyMap()))
        assertEquals(1, queue.size())
        queue.flush()
        scope.advanceUntilIdle()
        assertEquals(1, mockSender.sentPayloads.size)
        assertTrue(mockSender.sentPayloads[0].contains("flush_test"))
        assertEquals(0, queue.size())
    }

    @Test
    fun flush_withBatchSender_failure_keepsEventsForRetry() = runTest {
        val mockSender = MockEventBatchSender(returnSuccess = false)
        val (queue, scope) = createQueue(batchSender = mockSender)
        queue.enqueue(CustomEvent("flush_fail", emptyMap()))
        assertEquals(1, queue.size())
        queue.flush()
        scope.advanceUntilIdle()
        assertEquals(1, mockSender.sentPayloads.size)
        assertTrue(mockSender.sentPayloads[0].contains("flush_fail"))
        assertEquals(1, queue.size())
    }
}
