package com.pulsekit.core.api.backpressure

import com.pulsekit.core.api.events.PulseEvent

/**
 * Simplified priority system for event backpressure.
 *
 * This implementation focuses on the core needs of a telemetry SDK:
 * - Critical events must never be dropped
 * High priority events are preserved when possible
 * Low priority events are dropped first under pressure
 */
internal object SimplifiedPriorityCalculator {

    /**
     * Calculate priority for an event.
     *
     * Simplified logic focusing on the most important distinctions.
     */
    fun calculatePriority(event: PulseEvent): EventPriority {
        return when (event) {
            is com.pulsekit.core.api.events.ErrorEvent -> EventPriority.CRITICAL
            is com.pulsekit.core.api.events.LifecycleEvent -> EventPriority.HIGH
            is com.pulsekit.core.api.events.SessionEvent -> EventPriority.HIGH
            is com.pulsekit.core.api.events.PerformanceEvent -> {
                when {
                    event.metric.contains("error") -> EventPriority.CRITICAL
                    event.metric.contains("crash") -> EventPriority.CRITICAL
                    event.metric.contains("startup") -> EventPriority.HIGH
                    event.metric.contains("memory") -> EventPriority.HIGH
                    else -> EventPriority.MEDIUM
                }
            }
            is com.pulsekit.core.api.events.EngagementEvent -> {
                when {
                    event.action == com.pulsekit.core.api.events.EngagementAction.ERROR -> EventPriority.CRITICAL
                    event.action == com.pulsekit.core.api.events.EngagementAction.CRASH -> EventPriority.CRITICAL
                    event.action == com.pulsekit.core.api.events.EngagementAction.SESSION_START -> EventPriority.HIGH
                    event.action == com.pulsekit.core.api.events.EngagementAction.SESSION_END -> EventPriority.HIGH
                    else -> EventPriority.MEDIUM
                }
            }
            is com.pulsekit.core.api.events.CustomEvent -> {
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
}
