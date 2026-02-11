package com.pulsekit.core.api.storage

import com.pulsekit.core.api.config.PulseKitConfig
import com.pulsekit.core.api.events.PulseEvent
import com.pulsekit.core.api.errors.PulseKitError
import com.pulsekit.core.api.networking.EventBatchSender
import com.pulsekit.core.api.backpressure.SimplifiedBackpressureManager
import com.pulsekit.core.api.backpressure.SimplifiedPriorityCalculator
import com.pulsekit.core.api.backpressure.EventPriority
import com.pulsekit.core.api.backpressure.SimplifiedPriorityEvent
import com.pulsekit.core.api.backpressure.SimplifiedMetricsCollector
import com.pulsekit.core.api.logging.PulseKitLogger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * In-memory event queue with offline-first capabilities and simplified backpressure management.
 * 
 * Events are queued locally and flushed according to configuration.
 * This implementation includes comprehensive backpressure handling to prevent
 * unbounded memory growth while maintaining app stability under extreme load.
 */
public class EventQueue(
    private val config: PulseKitConfig,
    private val scope: CoroutineScope,
    private val batchSender: EventBatchSender? = null
) {

    private val events: MutableList<SimplifiedPriorityEvent> = mutableListOf()
    private val eventsMutex = Mutex()
    private val _eventFlow = MutableSharedFlow<PulseEvent>()
    private var isProcessing: Boolean = false

    // Simplified backpressure management
    private val backpressureManager = SimplifiedBackpressureManager(
        config.backpressureConfig,
        SimplifiedMetricsCollector()
    )
    
    /**
     * Flow of events ready for processing.
     */
    public val eventFlow: SharedFlow<PulseEvent> = _eventFlow.asSharedFlow()
    
    /**
     * Add an event to the queue with simplified backpressure handling.
     * 
     * @param event The event to queue
     */
    public fun enqueue(event: PulseEvent) {
        val priority = SimplifiedPriorityCalculator.calculatePriority(event)
        val priorityEvent = SimplifiedPriorityEvent(event, priority)

        runBlocking {
            eventsMutex.withLock {
                // Apply backpressure if needed
                val maxMemorySize = config.backpressureConfig.maxInMemoryQueueSize
                if (events.size >= maxMemorySize) {
                    val droppedCount = backpressureManager.applyBackpressure(
                        events,
                        maxMemorySize,
                        "memory"
                    )
                    if (config.enableDebugLogging && droppedCount > 0) {
                        PulseKitLogger.log("PulseKit", "Dropped $droppedCount events due to memory pressure")
                    }
                }
                events.add(priorityEvent)
                backpressureManager.updateUtilization(
                    events.size,
                    maxMemorySize,
                    0,
                    config.backpressureConfig.maxDiskQueueSize
                )
            }
        }

        // Emit event for immediate processing if enabled
        if (config.enableOfflineQueueing) {
            scope.launch {
                _eventFlow.emit(event)
            }
        }
    }
    
    /**
     * Get the next batch of events for processing.
     * 
     * @param batchSize Maximum number of events to return
     * @return List of events, may be empty if no events are available
     */
    public fun getNextBatch(batchSize: Int = 50): List<PulseEvent> = runBlocking {
        eventsMutex.withLock {
            if (events.isEmpty()) {
                return@runBlocking emptyList()
            }
            val now = Clock.System.now()
            val validEvents = events.filter { priorityEvent ->
                val age = now - priorityEvent.timestamp
                age <= config.maxEventAge
            }
            val expiredCount = events.size - validEvents.size
            if (expiredCount > 0) {
                events.removeAll { priorityEvent ->
                    val age = now - priorityEvent.timestamp
                    age > config.maxEventAge
                }
                if (config.enableDebugLogging && expiredCount > 0) {
                    PulseKitLogger.log("PulseKit", "Removed $expiredCount expired events")
                }
            }
            val sortedEvents = validEvents.sortedWith(
                compareByDescending<SimplifiedPriorityEvent> { it.priority.value }
                    .thenBy { it.timestamp }
            )
            sortedEvents.take(batchSize).map { it.event }
        }
    }
    
    /**
     * Mark events as successfully processed and remove them from the queue.
     *
     * @param processedEvents The events that were successfully processed
     */
    public fun markProcessed(processedEvents: List<PulseEvent>) = runBlocking {
        eventsMutex.withLock {
            processedEvents.forEach { event ->
                events.removeAll { it.event.eventId == event.eventId }
            }
            backpressureManager.updateUtilization(
                events.size,
                config.backpressureConfig.maxInMemoryQueueSize,
                0,
                config.backpressureConfig.maxDiskQueueSize
            )
        }
    }
    
    /**
     * Mark an event as failed and increment retry count.
     * 
     * @param event The event that failed to process
     */
    public fun markFailed(event: PulseEvent) = runBlocking {
        eventsMutex.withLock {
            val index = events.indexOfFirst { it.event.eventId == event.eventId }
            if (index >= 0) {
                val priorityEvent = events[index]
                val maxRetries = config.backpressureConfig.maxEventRetries
                if (priorityEvent.retryCount >= maxRetries) {
                    events.removeAt(index)
                    if (config.enableDebugLogging) {
                        PulseKitLogger.log("PulseKit", "Event ${event.eventId} exceeded max retries, dropping")
                    }
                } else {
                    val updatedEvent = priorityEvent.copy(retryCount = priorityEvent.retryCount + 1)
                    events[index] = updatedEvent
                }
            }
        }
    }
    
    /**
     * Get the current number of queued events.
     */
    public fun size(): Int = runBlocking { eventsMutex.withLock { events.size } }

    /**
     * Check if the queue is empty.
     */
    public fun isEmpty(): Boolean = runBlocking { eventsMutex.withLock { events.isEmpty() } }

    /**
     * Check if the queue is full.
     */
    public fun isFull(): Boolean = size() >= config.backpressureConfig.maxInMemoryQueueSize
    
    /**
     * Check if backpressure is active.
     */
    public fun isBackpressureActive(): Boolean {
        return backpressureManager.shouldApplyBackpressure(
            events.size, 
            config.backpressureConfig.maxInMemoryQueueSize
        )
    }
    
    /**
     * Clear all events from the queue.
     */
    public fun clear() = runBlocking {
        eventsMutex.withLock {
            events.clear()
            backpressureManager.updateUtilization(
                0,
                config.backpressureConfig.maxInMemoryQueueSize,
                0,
                config.backpressureConfig.maxDiskQueueSize
            )
            backpressureManager.reset()
        }
    }
    
    /**
     * Force flush all queued events.
     * 
     * This attempts to process all events immediately.
     */
    public fun flush() {
        if (isProcessing || events.isEmpty()) {
            return
        }
        
        isProcessing = true
        
        scope.launch {
            try {
                val batch = getNextBatch(Int.MAX_VALUE)
                if (batch.isEmpty()) return@launch
                if (batchSender != null) {
                    val jsonPayload = EventSerializer.serializeBatch(batch)
                    val success = runCatching { batchSender.sendBatch(jsonPayload) }.getOrElse { false }
                    if (success) {
                        markProcessed(batch)
                    } else {
                        batch.forEach { markFailed(it) }
                    }
                } else {
                    markProcessed(batch)
                }
            } finally {
                isProcessing = false
            }
        }
    }
    
    /**
     * Get queue statistics for debugging.
     */
    public fun getStats(): QueueStats = runBlocking {
        eventsMutex.withLock {
            val now = Clock.System.now()
            val eventsByAge = events.groupBy { priorityEvent ->
                val age = now - priorityEvent.timestamp
                when {
                    age < 1.minutes -> "fresh"
                    age < 5.minutes -> "recent"
                    age < 30.minutes -> "old"
                    else -> "stale"
                }
            }
            val eventsByPriority = events.groupBy { it.priority }
            QueueStats(
                totalEvents = events.size,
                isProcessing = isProcessing,
                eventsByAge = eventsByAge.mapValues { it.value.size },
                eventsByPriority = eventsByPriority.mapValues { it.value.size },
                oldestEventAge = events.minOfOrNull { now - it.timestamp },
                newestEventAge = events.maxOfOrNull { now - it.timestamp },
                backpressureMetrics = backpressureManager.getMetrics(),
                isBackpressureActive = isBackpressureActive()
            )
        }
    }
    
    /**
     * Get backpressure metrics.
     */
    public fun getBackpressureMetrics(): com.pulsekit.core.api.backpressure.SimplifiedMetrics {
        return backpressureManager.getMetrics()
    }
}

/**
 * Internal representation of a queued event with priority.
 */
private data class PriorityEvent(
    val event: PulseEvent,
    val priority: EventPriority,
    val timestamp: Instant = Clock.System.now(),
    val retryCount: Int = 0
)

/**
 * Statistics about the event queue.
 */
public data class QueueStats(
    public val totalEvents: Int,
    public val isProcessing: Boolean,
    public val eventsByAge: Map<String, Int>,
    public val eventsByPriority: Map<EventPriority, Int>,
    public val oldestEventAge: kotlin.time.Duration?,
    public val newestEventAge: kotlin.time.Duration?,
    public val backpressureMetrics: com.pulsekit.core.api.backpressure.SimplifiedMetrics,
    public val isBackpressureActive: Boolean
)
