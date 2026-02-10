package com.pulsekit.core.api.storage

import com.pulsekit.core.api.config.PulseKitConfig
import com.pulsekit.core.api.events.CustomEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventQueueTest {

    private fun createQueue(config: PulseKitConfig = PulseKitConfig()): Pair<EventQueue, TestScope> {
        val scope = TestScope(StandardTestDispatcher())
        val queue = EventQueue(config, scope)
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
                dropWhenDiskFull = true
            )
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
}
