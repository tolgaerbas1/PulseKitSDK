package com.pulsekit.core.api.storage

import com.pulsekit.core.api.events.CustomEvent
import com.pulsekit.core.api.events.EngagementAction
import com.pulsekit.core.api.events.EngagementEvent
import com.pulsekit.core.api.events.ErrorEvent
import com.pulsekit.core.api.events.ErrorType
import com.pulsekit.core.api.events.LifecycleAction
import com.pulsekit.core.api.events.LifecycleEvent
import com.pulsekit.core.api.events.PerformanceEvent
import com.pulsekit.core.api.events.PulseEvent
import com.pulsekit.core.api.events.SessionAction
import com.pulsekit.core.api.events.SessionEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

/**
 * Serializer for PulseEvent types using kotlinx.serialization.
 *
 * This handles the serialization and deserialization of events for
 * disk storage, maintaining type safety and all event properties.
 */
internal object EventSerializer {

    /**
     * Serialize a PulseEvent to JSON string.
     */
    fun serialize(event: PulseEvent): String {
        val serializableEvent = event.toSerializable()
        return json.encodeToString(SerializablePulseEvent.serializer(), serializableEvent)
    }
    
    /**
     * Deserialize a JSON string back to PulseEvent.
     */
    fun deserialize(jsonString: String): PulseEvent {
        val serializableEvent = json.decodeFromString(SerializablePulseEvent.serializer(), jsonString)
        return serializableEvent.toPulseEvent()
    }
    
    /**
     * Serialize a batch of events to a JSON array string.
     */
    fun serializeBatch(events: List<PulseEvent>): String {
        if (events.isEmpty()) return "[]"
        return "[" + events.joinToString(",") { serialize(it) } + "]"
    }

    /**
     * Get the event type name for a PulseEvent.
     */
    fun getEventType(event: PulseEvent): String {
        return when (event) {
            is CustomEvent -> "custom"
            is EngagementEvent -> "engagement"
            is LifecycleEvent -> "lifecycle"
            is PerformanceEvent -> "performance"
            is ErrorEvent -> "error"
            is SessionEvent -> "session"
        }
    }
}

/**
 * Serializable version of PulseEvent for JSON serialization.
 */
@Serializable
internal data class SerializablePulseEvent(
    val eventType: String,
    val eventId: String,
    val timestamp: String,
    val eventName: String,
    val metadata: Map<String, String>,
    val data: JsonElement? = null
) {
    
    fun toPulseEvent(): PulseEvent {
        return when (eventType) {
            "custom" -> data?.let { json.decodeFromJsonElement<CustomEventData>(it) }
                ?.toCustomEvent(eventId, timestamp, eventName, metadata)
                ?: throw IllegalArgumentException("Missing custom event data")
            
            "engagement" -> data?.let { json.decodeFromJsonElement<EngagementEventData>(it) }
                ?.toEngagementEvent(eventId, timestamp, eventName, metadata)
                ?: throw IllegalArgumentException("Missing engagement event data")
            
            "lifecycle" -> data?.let { json.decodeFromJsonElement<LifecycleEventData>(it) }
                ?.toLifecycleEvent(eventId, timestamp, eventName, metadata)
                ?: throw IllegalArgumentException("Missing lifecycle event data")
            
            "performance" -> data?.let { json.decodeFromJsonElement<PerformanceEventData>(it) }
                ?.toPerformanceEvent(eventId, timestamp, eventName, metadata)
                ?: throw IllegalArgumentException("Missing performance event data")
            
            "error" -> data?.let { json.decodeFromJsonElement<ErrorEventData>(it) }
                ?.toErrorEvent(eventId, timestamp, eventName, metadata)
                ?: throw IllegalArgumentException("Missing error event data")
            
            "session" -> data?.let { json.decodeFromJsonElement<SessionEventData>(it) }
                ?.toSessionEvent(eventId, timestamp, eventName, metadata)
                ?: throw IllegalArgumentException("Missing session event data")
            
            else -> throw IllegalArgumentException("Unknown event type: $eventType")
        }
    }
}

/**
 * Extension function to convert PulseEvent to SerializablePulseEvent.
 */
private fun PulseEvent.toSerializable(): SerializablePulseEvent {
    val data = when (this) {
        is CustomEvent -> CustomEventData(
            value = value,
            category = category
        ).let { json.encodeToJsonElement(CustomEventData.serializer(), it) }
        
        is EngagementEvent -> EngagementEventData(
            action = action.name,
            target = target,
            duration = duration?.inWholeMilliseconds
        ).let { json.encodeToJsonElement(EngagementEventData.serializer(), it) }
        
        is LifecycleEvent -> LifecycleEventData(
            action = action.name,
            component = component
        ).let { json.encodeToJsonElement(LifecycleEventData.serializer(), it) }
        
        is PerformanceEvent -> PerformanceEventData(
            metric = metric,
            value = value,
            unit = unit
        ).let { json.encodeToJsonElement(PerformanceEventData.serializer(), it) }
        
        is ErrorEvent -> ErrorEventData(
            errorType = errorType.name,
            message = message,
            stackTrace = stackTrace,
            isFatal = isFatal
        ).let { json.encodeToJsonElement(ErrorEventData.serializer(), it) }
        
        is SessionEvent -> SessionEventData(
            action = action.name,
            sessionId = sessionId.value
        ).let { json.encodeToJsonElement(SessionEventData.serializer(), it) }
    }
    
    return SerializablePulseEvent(
        eventType = EventSerializer.getEventType(this),
        eventId = eventId.value,
        timestamp = timestamp.toString(),
        eventName = eventName,
        metadata = metadata,
        data = data
    )
}

// Serializable data classes for each event type
@Serializable
private data class CustomEventData(
    val value: Double? = null,
    val category: String? = null
) {
    fun toCustomEvent(eventId: String, timestamp: String, eventName: String, metadata: Map<String, String>): CustomEvent {
        return CustomEvent(
            eventName = eventName,
            metadata = metadata,
            value = value,
            category = category
        )
    }
}

@Serializable
private data class EngagementEventData(
    val action: String,
    val target: String? = null,
    val duration: Long? = null
) {
    fun toEngagementEvent(eventId: String, timestamp: String, eventName: String, metadata: Map<String, String>): EngagementEvent {
        return EngagementEvent(
            action = EngagementAction.valueOf(action),
            target = target,
            duration = duration?.milliseconds,
            metadata = metadata
        )
    }
}

@Serializable
private data class LifecycleEventData(
    val action: String,
    val component: String
) {
    fun toLifecycleEvent(eventId: String, timestamp: String, eventName: String, metadata: Map<String, String>): LifecycleEvent {
        return LifecycleEvent(
            action = LifecycleAction.valueOf(action),
            component = component,
            metadata = metadata
        )
    }
}

@Serializable
private data class PerformanceEventData(
    val metric: String,
    val value: Double,
    val unit: String
) {
    fun toPerformanceEvent(eventId: String, timestamp: String, eventName: String, metadata: Map<String, String>): PerformanceEvent {
        return PerformanceEvent(
            metric = metric,
            value = value,
            unit = unit,
            metadata = metadata
        )
    }
}

@Serializable
private data class ErrorEventData(
    val errorType: String,
    val message: String,
    val stackTrace: String? = null,
    val isFatal: Boolean = false
) {
    fun toErrorEvent(eventId: String, timestamp: String, eventName: String, metadata: Map<String, String>): ErrorEvent {
        return ErrorEvent(
            errorType = ErrorType.valueOf(errorType),
            message = message,
            stackTrace = stackTrace,
            isFatal = isFatal,
            metadata = metadata
        )
    }
}

@Serializable
private data class SessionEventData(
    val action: String,
    val sessionId: String
) {
    fun toSessionEvent(eventId: String, timestamp: String, eventName: String, metadata: Map<String, String>): SessionEvent {
        return SessionEvent(
            action = SessionAction.valueOf(action),
            sessionId = com.pulsekit.core.api.session.SessionId(sessionId),
            metadata = metadata
        )
    }
}
