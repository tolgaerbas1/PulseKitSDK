package com.pulsekit.core.api.backpressure

import com.pulsekit.core.api.events.PulseEvent
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Simplified backpressure manager for queue overflow handling.
 * 
 * This implementation focuses on the core needs of a telemetry SDK:
 * - Prevents unbounded memory growth
 * - Ensures predictable behavior under load
 * - Preserves critical events
 */
internal class SimplifiedBackpressureManager(
    private val config: BackpressureConfig,
    private val metrics: SimplifiedMetricsCollector
) {
    
    /**
     * Apply backpressure to a queue when it exceeds capacity.
     */
    fun applyBackpressure(
        queue: MutableList<SimplifiedPriorityEvent>,
        capacity: Int,
        queueType: String
    ): Int {
        val eventsToDrop = queue.size - capacity
        if (eventsToDrop <= 0) return 0

        val droppedCount = when (config.dropPolicy) {
            DropPolicy.DROP_OLDEST -> dropOldest(queue, eventsToDrop)
            DropPolicy.DROP_NEWEST -> dropNewest(queue, eventsToDrop)
            DropPolicy.DROP_LOW_PRIORITY -> dropLowPriority(queue, eventsToDrop)
        }

        metrics.recordMemoryDrop(droppedCount, "queue_overflow")
        return droppedCount
    }
    
    /**
     * Drop oldest events from the queue.
     */
    private fun dropOldest(queue: MutableList<SimplifiedPriorityEvent>, count: Int): Int {
        queue.sortBy { it.timestamp }
        repeat(count) {
            if (queue.isNotEmpty()) {
                queue.removeFirstOrNull()
            }
        }
        return count
    }
    
    /**
     * Drop newest events from the queue.
     */
    private fun dropNewest(queue: MutableList<SimplifiedPriorityEvent>, count: Int): Int {
        queue.sortByDescending { it.timestamp }
        repeat(count) {
            if (queue.isNotEmpty()) {
                queue.removeFirstOrNull()
            }
        }
        return count
    }
    
    /**
     * Drop lowest priority events from the queue.
     */
    private fun dropLowPriority(queue: MutableList<SimplifiedPriorityEvent>, count: Int): Int {
        // Sort by priority (lowest first) then by timestamp (oldest first)
        queue.sortWith(
            compareBy<SimplifiedPriorityEvent> { it.priority.value }
                .thenBy { it.timestamp }
        )
        
        var dropped = 0
        repeat(count) {
            if (queue.isNotEmpty()) {
                queue.removeAt(0)
                dropped++
            }
        }
        return dropped
    }
    
    /**
     * Check if backpressure should be applied.
     */
    fun shouldApplyBackpressure(currentSize: Int, capacity: Int): Boolean {
        return currentSize > (capacity * config.backpressureThreshold)
    }
    
    /**
     * Get current metrics.
     */
    fun getMetrics(): SimplifiedMetrics {
        return metrics.getMetrics()
    }
    
    /**
     * Update utilization metrics.
     */
    fun updateUtilization(
        memorySize: Int,
        memoryCapacity: Int,
        diskSize: Int,
        diskCapacity: Int
    ) {
        metrics.updateUtilization(memorySize, memoryCapacity, diskSize, diskCapacity)
    }
    
    fun reset() {
        metrics.reset()
    }
}

/**
 * Simplified priority event wrapper.
 */
data class SimplifiedPriorityEvent(
    val event: PulseEvent,
    val priority: EventPriority,
    val timestamp: Instant = Clock.System.now(),
    var retryCount: Int = 0
)
