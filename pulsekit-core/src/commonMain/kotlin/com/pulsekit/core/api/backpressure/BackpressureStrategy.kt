package com.pulsekit.core.api.backpressure

import com.pulsekit.core.api.events.PulseEvent

/**
 * Backpressure strategy for managing event queue overflow.
 * 
 * This system ensures predictable behavior under extreme load
 * while maintaining app stability and data integrity.
 */

/**
 * Drop policies for handling queue overflow.
 */
enum class DropPolicy {
    /**
     * Drop the oldest events first (default).
     * 
     * Trade-off: Loses historical data but preserves recent events.
     * Use case: When recent events are more valuable than historical ones.
     */
    DROP_OLDEST,
    
    /**
     * Drop the newest events first.
     * 
     * Trade-off: Preserves historical data but loses recent events.
     * Use case: When historical continuity is more important.
     */
    DROP_NEWEST,
    
    /**
     * Drop lowest priority events first.
     * 
     * Trade-off: Preserves high-priority events, drops low-priority ones.
     * Use case: When event importance varies significantly.
     */
    DROP_LOW_PRIORITY
}

/**
 * Priority levels for events.
 * 
 * Higher priority events are less likely to be dropped under backpressure.
 */
enum class EventPriority {
    /**
     * Critical events - never dropped unless absolutely necessary.
     * Examples: Error events, crash reports, security events.
     */
    CRITICAL(4),
    
    /**
     * High priority events - dropped only under extreme pressure.
     * Examples: Lifecycle events, session events, performance metrics.
     */
    HIGH(3),
    
    /**
     * Medium priority events - dropped before critical/high priority.
     * Examples: Engagement events, user interactions, custom business events.
     */
    MEDIUM(2),
    
    /**
     * Low priority events - dropped first under backpressure.
     * Examples: Debug events, verbose logging, optional analytics.
     */
    LOW(1);
    
    internal val value: Int
    
    constructor(value: Int) {
        this.value = value
    }
}

/**
 * Backpressure configuration for queue management.
 */
data class BackpressureConfig(
    /**
     * Maximum number of events in memory queue.
     * Default: 1000 events
     */
    val maxInMemoryQueueSize: Int = 1000,
    
    /**
     * Maximum number of events in disk queue.
     * Default: 10000 events
     */
    val maxDiskQueueSize: Int = 10000,
    
    /**
     * Drop policy for queue overflow.
     * Default: DROP_OLDEST
     */
    val dropPolicy: DropPolicy = DropPolicy.DROP_OLDEST,
    
    /**
     * Enable priority-based dropping.
     * Default: true
     */
    val enablePriorityDropping: Boolean = true,
    
    /**
     * Threshold for triggering backpressure (percentage of capacity).
     * Default: 0.9 (90%)
     */
    val backpressureThreshold: Double = 0.9,
    
    /**
     * Whether to drop events when disk is full.
     * Default: true (prefer stability over data completeness)
     */
    val dropWhenDiskFull: Boolean = true,

    /**
     * Maximum retry attempts for a failed event before dropping.
     * Default: 3
     */
    val maxEventRetries: Int = 3
)

/**
 * Backpressure metrics for observability.
 */
internal data class BackpressureMetrics(
    /**
     * Number of events dropped from memory queue.
     */
    val memoryDroppedCount: Long = 0,
    
    /**
     * Number of events dropped from disk queue.
     */
    val diskDroppedCount: Long = 0,
    
    /**
     * Number of events dropped by priority.
     */
    val priorityDroppedCount: Long = 0,
    
    /**
     * Current memory queue utilization.
     */
    val memoryUtilization: Double = 0.0,
    
    /**
     * Current disk queue utilization.
     */
    val diskUtilization: Double = 0.0,
    
    /**
     * Last drop reason.
     */
    val lastDropReason: String? = null,
    
    /**
     * Timestamp of last drop event.
     */
    val lastDropTimestamp: kotlinx.datetime.Instant? = null
)

/**
 * Priority calculator for events.
 */
internal object EventPriorityCalculator {
    
    /**
     * Calculate priority for an event based on its type and characteristics.
     */
    fun calculatePriority(event: PulseEvent): EventPriority {
        return when (event) {
            is com.pulsekit.core.api.events.ErrorEvent -> EventPriority.CRITICAL
            is com.pulsekit.core.api.events.LifecycleEvent -> EventPriority.HIGH
            is com.pulsekit.core.api.events.SessionEvent -> EventPriority.HIGH
            is com.pulsekit.core.api.events.PerformanceEvent -> {
                // Performance events can be critical if they indicate problems
                if (event.metric.contains("error") || event.metric.contains("crash")) {
                    EventPriority.CRITICAL
                } else if (event.metric.contains("startup") || event.metric.contains("memory")) {
                    EventPriority.HIGH
                } else {
                    EventPriority.MEDIUM
                }
            }
            is com.pulsekit.core.api.events.EngagementEvent -> {
                // Engagement events are generally medium priority
                when (event.action) {
                    com.pulsekit.core.api.events.EngagementAction.ERROR -> EventPriority.CRITICAL
                    com.pulsekit.core.api.events.EngagementAction.CRASH -> EventPriority.CRITICAL
                    com.pulsekit.core.api.events.EngagementAction.SESSION_START -> EventPriority.HIGH
                    com.pulsekit.core.api.events.EngagementAction.SESSION_END -> EventPriority.HIGH
                    else -> EventPriority.MEDIUM
                }
            }
            is com.pulsekit.core.api.events.CustomEvent -> {
                // Custom events default to medium priority
                // Can be adjusted based on event name or metadata
                when {
                    event.eventName.contains("error") -> EventPriority.CRITICAL
                    event.eventName.contains("crash") -> EventPriority.CRITICAL
                    event.eventName.contains("lifecycle") -> EventPriority.HIGH
                    event.eventName.contains("session") -> EventPriority.HIGH
                    event.eventName.contains("debug") -> EventPriority.LOW
                    event.eventName.contains("trace") -> EventPriority.LOW
                    else -> EventPriority.MEDIUM
                }
            }
        }
    }
    
    /**
     * Check if an event should be high priority based on metadata.
     */
    private fun isHighPriorityByMetadata(metadata: Map<String, String>): Boolean {
        return metadata.any { (key, value) ->
            key.contains("error") || 
            key.contains("critical") ||
            value.contains("error") ||
            value.contains("critical")
        }
    }
    
    /**
     * Check if an event should be low priority based on metadata.
     */
    private fun isLowPriorityByMetadata(metadata: Map<String, String>): Boolean {
        return metadata.any { (key, value) ->
            key.contains("debug") ||
            key.contains("trace") ||
            key.contains("verbose") ||
            value.contains("debug") ||
            value.contains("trace") ||
            value.contains("verbose")
        }
    }
}

/**
 * Backpressure manager for queue overflow handling.
 */
internal class BackpressureManager(
    private val config: BackpressureConfig
) {
    
    private var metrics = BackpressureMetrics()
    
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
            DropPolicy.DROP_OLDEST -> dropOldest(queue, eventsToDrop)
            DropPolicy.DROP_NEWEST -> dropNewest(queue, eventsToDrop)
            DropPolicy.DROP_LOW_PRIORITY -> dropLowPriority(queue, eventsToDrop)
        }
        
        updateMetrics(droppedCount, queueType, "queue_overflow")
        return droppedCount
    }
    
    /**
     * Drop oldest events from the queue.
     */
    private fun dropOldest(queue: MutableList<PriorityEvent>, count: Int): Int {
        // Sort by timestamp (oldest first) and drop the oldest
        queue.sortBy { it.event.timestamp }
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
        queue.sortByDescending { it.event.timestamp }
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
        if (!config.enablePriorityDropping) {
            return dropOldest(queue, count)
        }
        
        // Sort by priority (lowest first) then by timestamp
        queue.sortWith(compareBy<PriorityEvent> { it.priority.value }.thenBy { it.event.timestamp })
        
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
     * Get current backpressure metrics.
     */
    fun getMetrics(): BackpressureMetrics = metrics.copy()
    
    /**
     * Update backpressure metrics.
     */
    private fun updateMetrics(droppedCount: Int, queueType: String, reason: String) {
        metrics = metrics.copy(
            memoryDroppedCount = if (queueType == "memory") metrics.memoryDroppedCount + droppedCount else metrics.memoryDroppedCount,
            diskDroppedCount = if (queueType == "disk") metrics.diskDroppedCount + droppedCount else metrics.diskDroppedCount,
            priorityDroppedCount = if (config.dropPolicy == DropPolicy.DROP_LOW_PRIORITY) metrics.priorityDroppedCount + droppedCount else metrics.priorityDroppedCount,
            lastDropReason = reason,
            lastDropTimestamp = kotlinx.datetime.Clock.System.now()
        )
    }
    
    /**
     * Update utilization metrics.
     */
    fun updateUtilization(memorySize: Int, memoryCapacity: Int, diskSize: Int, diskCapacity: Int) {
        metrics = metrics.copy(
            memoryUtilization = memorySize.toDouble() / memoryCapacity,
            diskUtilization = diskSize.toDouble() / diskCapacity
        )
    }
}

/**
 * Event with priority information.
 */
internal data class PriorityEvent(
    val event: PulseEvent,
    val priority: EventPriority,
    val timestamp: kotlinx.datetime.Instant = kotlinx.datetime.Clock.System.now(),
    val retryCount: Int = 0
)
