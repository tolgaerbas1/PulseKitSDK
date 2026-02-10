package com.pulsekit.core.api.events

import com.pulsekit.core.api.config.PulseKitConfig
import com.pulsekit.core.api.errors.PulseKitError
import com.pulsekit.core.api.logging.PulseKitLogger
import com.pulsekit.core.api.storage.EventQueue
import com.pulsekit.core.api.flags.PulseKitFeatureFlags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.plus
import kotlin.toString

/**
 * Processes events and manages the event pipeline.
 * 
 * This class handles the core event processing logic, including
 * validation, enrichment, and queuing, with feature flag support.
 */
public class EventProcessor private constructor(
    private val config: PulseKitConfig,
    private val eventQueue: EventQueue,
    private val scope: CoroutineScope,
    private val flagProvider: FlagProvider
) {
    
    private var isStarted: Boolean = false
    
    init {
        start()
    }
    
    companion object {
        fun create(
            config: PulseKitConfig,
            eventQueue: EventQueue,
            scope: CoroutineScope,
            flagProvider: FlagProvider
        ): EventProcessor {
            return EventProcessor(config, eventQueue, scope, flagProvider)
        }
    }
    
    /**
     * Process a single event.
     * 
     * @param event The event to process
     * @throws PulseKitError.Event if processing fails
     */
    public fun process(event: PulseEvent) {
        try {
            validateEvent(event)
            val enrichedEvent = enrichEvent(event)
            processEventWithFlags(enrichedEvent)
        } catch (e: PulseKitError) {
            // Re-throw PulseKit errors as-is
            throw e as Throwable
        } catch (e: Exception) {
            throw PulseKitError.Event.ProcessingFailed(
                message = "Unexpected error processing event: ${e.message}",
                cause = e
            )
        }
    }
    
    /**
     * Start the event processor.
     * 
     * This begins listening to the event queue and processing events.
     */
    public fun start() {
        if (isStarted) return
        
        isStarted = true
        
        // Listen to events from the queue and process them
        eventQueue.eventFlow
            .onEach { event ->
                // Process events from queue with feature flag support
                processEventWithFlags(event)
            }
            .catch { error ->
                // Handle processing errors
                if (config.enableDebugLogging) {
                    PulseKitLogger.log("PulseKit", "Event processing error: ${error.message}")
                }
            }
            .launchIn(scope)
    }
    
    /**
     * Stop the event processor.
     */
    public fun stop() {
        isStarted = false
        // Cancel any ongoing processing
    }
    
    /**
     * Process an event with feature flag support.
     */
    private fun processEventWithFlags(event: PulseEvent) {
        val shouldCompress = flagProvider.getBooleanFlag(PulseKitFeatureFlags.EVENT_COMPRESSION)
        val maxBatchSize = flagProvider.getIntegerFlag(PulseKitFeatureFlags.EVENT_BATCH_SIZE)
        val useExperimentalRetry = flagProvider.getBooleanFlag(PulseKitFeatureFlags.EXPERIMENTAL_RETRY_LOGIC)
        val deduplicateEvents = flagProvider.getBooleanFlag(PulseKitFeatureFlags.EVENT_DEDUPLICATION)

        val augmentedMetadata = event.metadata + mapOf(
            "compressed" to shouldCompress.toString(),
            "max_batch_size" to maxBatchSize.toString(),
            "experimental_retry" to useExperimentalRetry.toString(),
            "deduplicate" to deduplicateEvents.toString()
        )

        val processedEvent = event.withMetadata(augmentedMetadata)

        eventQueue.enqueue(processedEvent)
    }
    
    /**
     * Apply feature flags to an event.
     */
    private fun applyFeatureFlags(event: PulseEvent): PulseEvent {
        // Apply compression flag
        val shouldCompress = flagProvider.getBooleanFlag(PulseKitFeatureFlags.EVENT_COMPRESSION)
        
        // Apply batch size limit
        val maxBatchSize = flagProvider.getIntegerFlag(PulseKitFeatureFlags.EVENT_BATCH_SIZE)
        
        // Apply experimental retry logic
        val useExperimentalRetry = flagProvider.getBooleanFlag(PulseKitFeatureFlags.EXPERIMENTAL_RETRY_LOGIC)
        
        // Apply event deduplication
        val deduplicateEvents = flagProvider.getBooleanFlag(PulseKitFeatureFlags.EVENT_DEDUPLICATION)
        
        return event.withMetadata(
            event.metadata + mapOf(
                "compressed" to shouldCompress.toString(),
                "max_batch_size" to maxBatchSize.toString(),
                "experimental_retry" to useExperimentalRetry.toString(),
                "deduplicate" to deduplicateEvents.toString()
            )
        )
    }
    
    /**
     * Validate an event before processing.
     */
    private fun validateEvent(event: PulseEvent) {
        // Check event size (rough estimation)
        val estimatedSize = event.eventName.length + 
            event.metadata.values.sumOf { it.length } +
            100 // Base overhead
        
        // Apply max queue size limit from feature flags
        val maxQueueSize = flagProvider.getIntegerFlag(PulseKitFeatureFlags.MAX_QUEUE_SIZE)
        
        if (estimatedSize > 1024 * 32) { // 32KB limit
            throw PulseKitError.Event.EventTooLarge(
                eventSize = estimatedSize,
                maxSize = 1024 * 32
            )
        }
        
        if (estimatedSize > maxQueueSize * 10) { // Soft limit
        if (config.enableDebugLogging) {
            PulseKitLogger.log("PulseKit", "Large event detected (${estimatedSize} bytes), consider increasing queue size")
        }
        }
        
        // Validate event name
        if (event.eventName.isBlank()) {
            throw PulseKitError.Event.InvalidEvent("Event name cannot be blank")
        }
        
        // Validate metadata keys and values
        event.metadata.forEach { (key, value) ->
            if (key.isBlank()) {
                throw PulseKitError.Event.InvalidEvent("Metadata key cannot be blank")
            }
            if (value.length > 1024) {
                throw PulseKitError.Event.InvalidEvent("Metadata value too long for key: $key")
            }
        }
    }
    
    /**
     * Enrich an event with additional context.
     */
    private fun enrichEvent(event: PulseEvent): PulseEvent {
        val enrichedMetadata = event.metadata.toMutableMap()
        enrichedMetadata.putAll(config.globalMetadata)
        enrichedMetadata.putAll(
            mapOf(
                "feature_flags_enabled" to "true",
                "compression_enabled" to flagProvider.getBooleanFlag(PulseKitFeatureFlags.EVENT_COMPRESSION).toString(),
                "batch_size" to flagProvider.getIntegerFlag(PulseKitFeatureFlags.EVENT_BATCH_SIZE).toString(),
                "retry_logic" to flagProvider.getBooleanFlag(PulseKitFeatureFlags.EXPONENTIAL_BACKOFF).toString(),
                "offline_queueing" to flagProvider.getBooleanFlag(PulseKitFeatureFlags.OFFLINE_QUEUEING).toString()
            )
        )

        return event.withMetadata(enrichedMetadata)
    }
}

/**
 * Provider interface for feature flag values.
 * 
 * This decouples the flag system from the rest of the SDK.
 */
interface FlagProvider {
    fun getBooleanFlag(flag: com.pulsekit.core.api.flags.FeatureFlag): Boolean
    fun getIntegerFlag(flag: com.pulsekit.core.api.flags.FeatureFlag): Long
    fun getDoubleFlag(flag: com.pulsekit.core.api.flags.FeatureFlag): Double
    fun getStringFlag(flag: com.pulsekit.core.api.flags.FeatureFlag): String
}
