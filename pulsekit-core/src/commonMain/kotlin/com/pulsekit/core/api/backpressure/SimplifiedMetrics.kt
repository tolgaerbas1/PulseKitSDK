package com.pulsekit.core.api.backpressure

import kotlinx.datetime.Instant

/**
 * Simplified metrics data structure.
 */
data class SimplifiedMetrics(
    val memoryDroppedCount: Long,
    val diskDroppedCount: Long,
    val priorityDroppedCount: Long,
    val lastDropReason: String?,
    val lastDropTime: Instant?,
    val memoryUtilization: Double,
    val diskUtilization: Double
)

/**
 * Simplified metrics for internal debugging and monitoring.
 * 
 * This focuses on essential metrics needed for debugging
 * without overengineering the telemetry SDK.
 */
internal class SimplifiedMetricsCollector {
    
    private var memoryDroppedCount: Long = 0
    private var diskDroppedCount: Long = 0
    private var priorityDroppedCount: Long = 0
    private var lastDropReason: String? = null
    private var lastDropTime: Instant? = null
    private var memoryUtilization: Double = 0.0
    private var diskUtilization: Double = 0.0
    
    /**
     * Record events dropped from memory queue.
     */
    fun recordMemoryDrop(count: Int, reason: String) {
        memoryDroppedCount += count
        lastDropReason = reason
        lastDropTime = kotlinx.datetime.Clock.System.now()
    }
    
    /**
     * Record events dropped from disk queue.
     */
    fun recordDiskDrop(count: Int, reason: String) {
        diskDroppedCount += count
        lastDropReason = reason
        lastDropTime = kotlinx.datetime.Clock.System.now()
    }
    
    /**
     * Record events dropped by priority.
     */
    fun recordPriorityDrop(count: Int, reason: String) {
        priorityDroppedCount += count
        lastDropReason = reason
        lastDropTime = kotlinx.datetime.Clock.System.now()
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
        memoryUtilization = memorySize.toDouble() / memoryCapacity
        diskUtilization = diskSize.toDouble() / diskCapacity
    }
    
    /**
     * Get current metrics.
     */
    fun getMetrics(): SimplifiedMetrics {
        return SimplifiedMetrics(
            memoryDroppedCount = memoryDroppedCount,
            diskDroppedCount = diskDroppedCount,
            priorityDroppedCount = priorityDroppedCount,
            lastDropReason = lastDropReason,
            lastDropTime = lastDropTime,
            memoryUtilization = memoryUtilization,
            diskUtilization = diskUtilization
        )
    }
    
    /**
     * Reset all metrics.
     */
    fun reset() {
        memoryDroppedCount = 0
        diskDroppedCount = 0
        priorityDroppedCount = 0
        lastDropReason = null
        lastDropTime = null
        memoryUtilization = 0.0
        diskUtilization = 0.0
    }
}
