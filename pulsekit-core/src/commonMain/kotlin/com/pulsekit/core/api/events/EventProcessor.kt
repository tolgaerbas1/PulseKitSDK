package com.pulsekit.core.api.events

import com.pulsekit.core.api.config.PulseKitConfig
import com.pulsekit.core.api.errors.PulseKitError
import com.pulsekit.core.api.storage.EventQueue
import com.pulsekit.core.api.flags.PulseKitFeatureFlags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Processes events and manages the event pipeline.
 * 
 * This class handles the core event processing logic, including
 * validation, enrichment, and queuing, with feature flag support.
 */
public class EventProcessor(
    private val config: PulseKitConfig,
    private val eventQueue: EventQueue,
    private val scope: CoroutineScope,
    private val flagProvider: FlagProvider
) {
    
    private var isStarted: Boolean = false
    
    init {
        start()
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
            throw e
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
                    println("PulseKit: Event processing error: ${error.message}")
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
        // Apply feature flag-based behavior changes
        val processedEvent = applyFeatureFlags(event)
        
        // Enqueue the processed event
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
        
        return when (event) {
            is CustomEvent -> {
                event.copy(
                    metadata = event.metadata + mapOf(
                        "compressed" to shouldCompress.toString(),
                        "max_batch_size" to maxBatchSize.toString(),
                        "experimental_retry" to useExperimentalRetry.toString(),
                        "deduplicate" to deduplicateEvents.toString()
                    )
                )
            }
            is EngagementEvent -> {
                event.copy(
                    metadata = event.metadata + mapOf(
                        "compressed" to shouldCompress.toString(),
                        "max_batch_size" to maxBatchSize.toString(),
                        "experimental_retry" to useExperimentalRetry.toString(),
                        "deduplicate" to deduplicateEvents.toString()
                    )
                )
            }
            is PerformanceEvent -> {
                event.copy(
                    metadata = event.metadata + mapOf(
                        "compressed" to shouldCompress.toString(),
                        "max_batch_size" to maxBatchSize.toString(),
                        "experimental_retry" to useExperimentalRetry.toString(),
                        "deduplicate" to deduplicateEvents.toString()
                    )
                )
            }
            is ErrorEvent -> {
                event.copy(
                    metadata = event.metadata + mapOf(
                        "compressed" to shouldCompress.toString(),
                        "max_batch_size" to maxBatchSize.toString(),
                        "experimental_retry" to useExperimentalRetry.toString(),
                        "deduplicate" to deduplicateEvents.toString()
                    )
                )
            }
            is LifecycleEvent -> {
                event.copy(
                    metadata = event.metadata + mapOf(
                        "compressed" to shouldCompress.toString(),
                        "max_batch_size" to maxBatchSize.toString(),
                        "experimental_retry" to useExperimentalRetry.toString(),
                        "deduplicate" to deduplicateEvents.toString()
                    )
                )
            }
            is SessionEvent -> {
                event.copy(
                    metadata = event.metadata + mapOf(
                        "compressed" to shouldCompress.toString(),
                        "max_batch_size" to maxBatchSize.toString(),
                        "experimental_retry" to useExperimentalRetry.toString(),
                        "deduplicate" to deduplicateEvents.toString()
                    )
                )
            }
        }
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
                println("PulseKit: Large event detected (${estimatedSize} bytes), consider increasing queue size")
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
        // Add global metadata
        val enrichedMetadata = event.metadata.toMutableMap()
        enrichedMetadata.putAll(config.globalMetadata)
        
        // Add feature flag metadata
        enrichedMetadata.putAll(
            "feature_flags_enabled" to "true",
            "compression_enabled" to flagProvider.getBooleanFlag(PulseKitFeatureFlags.EVENT_COMPRESSION).toString(),
            "batch_size" to flagProvider.getIntegerFlag(PulseKitFeatureFlags.EVENT_BATCH_SIZE).toString(),
            "retry_logic" to flagProvider.getBooleanFlag(PulseKitFeatureFlags.EXPONENTIAL_BACKOFF).toString(),
            "offline_queueing" to flagProvider.getBooleanFlag(PulseKitFeatureFlags.OFFLINE_QUEUEING).toString()
        )
        
        // TODO: Add session information
        // TODO: Add device information
        // TODO: Add app version information
        
        return when (event) {
            is CustomEvent -> event.copy(metadata = enrichedMetadata)
            is EngagementEvent -> event.copy(metadata = enrichedMetadata)
            is LifecycleEvent -> event.copy(metadata = enrichedMetadata)
            is PerformanceEvent -> event.copy(metadata = enrichedMetadata)
            is ErrorEvent -> event.copy(metadata = enrichedMetadata)
            is SessionEvent -> event.copy(metadata = enrichedMetadata)
        }
    }
}

/**
 * Provider interface for feature flag values.
 * 
 * This decouples the flag system from the rest of the SDK.
 */
internal interface FlagProvider {
    fun getBooleanFlag(flag: com.pulsekit.core.api.flags.FeatureFlag): Boolean
    fun getIntegerFlag(flag: com.pulsekit.core.api.flags.FeatureFlag): Long
    fun getDoubleFlag(flag: com.pulsekit.core.api.flags.FeatureFlag): Double
    fun getStringFlag(flag: com.pulsekit.core.api.flags.FeatureFlag): String
}
