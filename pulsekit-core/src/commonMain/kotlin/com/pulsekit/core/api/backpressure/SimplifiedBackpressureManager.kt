package com.pulsekit.core.api.backpressure

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Simplified backpressure manager for queue overflow handling.
 * 
 * This implementation focuses on the core needs of a telemetry SDK:
 * - Prevents unbounded memory growth
 * - Ensures predictable behavior under load
 * - Preserves critical events
 */
internal class SimplifiedBackpressureManager(
    private val config: com.pulsekit.core.api.backpressure.BackpressureConfig,
    private val metrics: SimplifiedMetrics
) {
    
    /**
     * Apply backpressure to a queue when it exceeds capacity.
     */
    fun applyBackpressure(
        queue: MutableList<PriorityEvent>,
        capacity: Int,
        queueType: String
    ): Int {
        val eventsToDrop = queue.size - capacity
        if (eventsToDrop <= 0) return 0
        
        val droppedCount = when (config.dropPolicy) {
            com.pulsekit.core.api.backpressure.DropPolicy.DROP_OLDEST -> dropOldest(queue, eventsToDrop)
            com.pulsekit.core.api.backpressure.DropPolicy.DROP_NEWEST -> dropNewest(queue, eventsToDrop)
            com.pulsekit.core.api.backpressure.DropPolicy.DROP_LOW_PRIORITY -> dropLowPriority(queue, eventsToDrop)
        }
        
        metrics.recordMemoryDrop(droppedCount, "queue_overflow")
        return droppedCount
    }
    
    /**
     * Drop oldest events from the queue.
     */
    private fun dropOldest(queue: MutableList<PriorityEvent>, count: Int): Int {
        // Sort by timestamp (oldest first) and drop the oldest
        queue.sortBy { it.timestamp }
        repeat(count) {
            if (queue.isNotEmpty()) {
                queue.removeAt(0)
            }
        }
        return count
    }
    
    /**
     * Drop newest events from the queue.
     */
    private fun dropNewest(queue: MutableList<PriorityEvent>, count: Int): Int {
        // Sort by timestamp (newest first) and drop the newest
        queue.sortByDescending { it.timestamp }
        repeat(count) {
            if (queue.isNotEmpty()) {
                queue.removeAt(0)
            }
        }
        return count
    }
    
    /**
     * Drop lowest priority events from the queue.
     */
    private fun dropLowPriority(queue: MutableList<PriorityEvent>, count: Int): Int {
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
}

/**
 * Simplified priority event wrapper.
 */
internal data class SimplifiedPriorityEvent(
    val event: com.pulsekit.core.api.events.PulseEvent,
    val priority: EventPriority,
    val timestamp: kotlinx.datetime.Instant = kotlinx.datetime.Clock.System.now()
)
