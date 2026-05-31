package com.pulsekit.core.api.storage

import com.pulsekit.core.api.backpressure.EventPriority
import com.pulsekit.core.api.backpressure.SimplifiedBackpressureManager
import com.pulsekit.core.api.backpressure.SimplifiedMetricsCollector
import com.pulsekit.core.api.backpressure.SimplifiedPriorityCalculator
import com.pulsekit.core.api.backpressure.SimplifiedPriorityEvent
import com.pulsekit.core.api.config.PulseKitConfig
import com.pulsekit.core.api.events.PulseEvent
import com.pulsekit.core.api.logging.PulseKitLogger
import com.pulsekit.core.api.networking.EventBatchSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Thread-safe event queue with optional disk-backed persistence.
 *
 * Events are queued locally and flushed according to configuration.
 * When a [DatabaseDriver] is provided via [setDatabaseDriver], events are
 * persisted to disk and survive app restarts.
 */
public class EventQueue(
    private val config: PulseKitConfig,
    private val scope: CoroutineScope,
    private val batchSender: EventBatchSender? = null,
) {

    private val events: MutableList<SimplifiedPriorityEvent> = mutableListOf()
    private val lock = ReentrantLock()
    private val _eventFlow = MutableSharedFlow<PulseEvent>()

    @Volatile
    private var isProcessing: Boolean = false

    @Volatile
    private var databaseDriver: DatabaseDriver? = null

    // Simplified backpressure management
    private val backpressureManager = SimplifiedBackpressureManager(
        config.backpressureConfig,
        SimplifiedMetricsCollector(),
    )

    /**
     * Flow of events ready for processing.
     */
    public val eventFlow: SharedFlow<PulseEvent> = _eventFlow.asSharedFlow()

    /**
     * Set the database driver for disk-backed persistence.
     *
     * Call this before [loadFromDisk] to enable persistence.
     * Calling without a driver (or null) runs the queue in memory-only mode.
     */
    public fun setDatabaseDriver(driver: DatabaseDriver?) {
        databaseDriver = driver
    }

    /**
     * Load previously persisted events from disk into the in-memory queue.
     *
     * Must be called after [setDatabaseDriver] and before events are enqueued.
     * On success, all stored events are inserted into the in-memory queue in
     * their original order and with their original retry counts preserved.
     */
    public suspend fun loadFromDisk() {
        val driver = databaseDriver ?: return
        driver.initialize()
        try {
            val diskEvents = driver.getEventBatch(Int.MAX_VALUE, excludeExpired = true)
            if (diskEvents.isNotEmpty()) {
                lock.withLock {
                    diskEvents.forEach { stored ->
                        val event = EventSerializer.deserialize(stored.eventData)
                        val priority = SimplifiedPriorityCalculator.calculatePriority(event)
                        events.add(
                            SimplifiedPriorityEvent(
                                event = event,
                                priority = priority,
                                timestamp = stored.queuedAt,
                                retryCount = stored.retryCount,
                            ),
                        )
                    }
                }
                if (config.enableDebugLogging) {
                    PulseKitLogger.log("PulseKit", "Loaded ${diskEvents.size} events from disk")
                }
            }
        } catch (e: Exception) {
            PulseKitLogger.log("PulseKit", "Failed to load events from disk: ${e.message}")
        }
    }

    /**
     * Add an event to the queue with simplified backpressure handling.
     *
     * @param event The event to queue
     */
    public fun enqueue(event: PulseEvent) {
        val priority = SimplifiedPriorityCalculator.calculatePriority(event)
        val priorityEvent = SimplifiedPriorityEvent(event, priority)

        lock.withLock {
            val maxMemorySize = config.backpressureConfig.maxInMemoryQueueSize
            events.add(priorityEvent)

            if (events.size > maxMemorySize) {
                val droppedCount = backpressureManager.applyBackpressure(
                    events,
                    maxMemorySize,
                    "memory",
                )
                if (config.enableDebugLogging && droppedCount > 0) {
                    PulseKitLogger.log("PulseKit", "Dropped $droppedCount events due to memory pressure")
                }
            }
            backpressureManager.updateUtilization(
                events.size,
                maxMemorySize,
                0,
                config.backpressureConfig.maxDiskQueueSize,
            )
        }

        // Persist to disk asynchronously (fire-and-forget, best-effort)
        val driver = databaseDriver
        if (driver != null) {
            scope.launch {
                try {
                    val stored = StoredEvent(
                        eventId = event.eventId.value,
                        eventType = EventSerializer.getEventType(event),
                        eventData = EventSerializer.serialize(event),
                        queuedAt = Clock.System.now(),
                        retryCount = 0,
                        expiresAt = Clock.System.now() + config.maxEventAge,
                    )
                    driver.insertEvent(stored)
                } catch (_: Exception) { }
            }
        }

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
    public fun getNextBatch(batchSize: Int = 50): List<PulseEvent> = lock.withLock {
        if (events.isEmpty()) {
            return emptyList()
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
                .thenBy { it.timestamp },
        )
        sortedEvents.take(batchSize).map { it.event }
    }

    /**
     * Mark events as successfully processed and remove them from the queue.
     *
     * @param processedEvents The events that were successfully processed
     */
    public fun markProcessed(processedEvents: List<PulseEvent>) {
        lock.withLock {
            processedEvents.forEach { event ->
                events.removeAll { it.event.eventId == event.eventId }
            }
            backpressureManager.updateUtilization(
                events.size,
                config.backpressureConfig.maxInMemoryQueueSize,
                0,
                config.backpressureConfig.maxDiskQueueSize,
            )
        }

        val driver = databaseDriver
        if (driver != null && processedEvents.isNotEmpty()) {
            scope.launch {
                try {
                    driver.deleteEvents(processedEvents.map { it.eventId.value })
                } catch (_: Exception) { }
            }
        }
    }

    /**
     * Mark an event as failed and increment retry count.
     *
     * @param event The event that failed to process
     */
    public fun markFailed(event: PulseEvent) {
        var newRetryCount: Int? = null
        lock.withLock {
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
                    val updated = priorityEvent.copy(retryCount = priorityEvent.retryCount + 1)
                    events[index] = updated
                    newRetryCount = updated.retryCount
                }
            }
        }

        val driver = databaseDriver
        val retry = newRetryCount
        if (driver != null && retry != null) {
            scope.launch {
                try {
                    driver.updateRetryCount(listOf(event.eventId.value), retry)
                } catch (_: Exception) { }
            }
        }
    }

    /**
     * Get the current number of queued events.
     */
    public fun size(): Int = lock.withLock { events.size }

    /**
     * Check if the queue is empty.
     */
    public fun isEmpty(): Boolean = lock.withLock { events.isEmpty() }

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
            config.backpressureConfig.maxInMemoryQueueSize,
        )
    }

    /**
     * Clear all events from the queue.
     */
    public fun clear() {
        lock.withLock {
            events.clear()
            backpressureManager.updateUtilization(
                0,
                config.backpressureConfig.maxInMemoryQueueSize,
                0,
                config.backpressureConfig.maxDiskQueueSize,
            )
            backpressureManager.reset()
        }

        val driver = databaseDriver
        if (driver != null) {
            scope.launch {
                try {
                    driver.clearAllEvents()
                } catch (_: Exception) { }
            }
        }
    }

    /**
     * Close the database driver and release resources.
     */
    public suspend fun dispose() {
        databaseDriver?.close()
        databaseDriver = null
    }

    /**
     * Force flush all queued events.
     *
     * This attempts to process all events immediately.
     */
    public fun flush() {
        val shouldStart = lock.withLock {
            if (isProcessing || events.isEmpty()) {
                false
            } else {
                isProcessing = true
                true
            }
        }
        if (!shouldStart) return

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
                lock.withLock {
                    isProcessing = false
                }
            }
        }
    }

    /**
     * Get queue statistics for debugging.
     */
    public fun getStats(): QueueStats = lock.withLock {
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
            isBackpressureActive = isBackpressureActive(),
        )
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
    val retryCount: Int = 0,
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
    public val isBackpressureActive: Boolean,
)
