package com.pulsekit.core.api.extensions

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Extension functions for Duration.
 */
val Int.hours: Duration get() = this.hours
val Int.minutes: Duration get() = this.minutes
val Long.hours: Duration get() = this.hours
val Long.minutes: Duration get() = this.minutes

/**
 * Convert Duration to milliseconds.
 */
val Duration.milliseconds: Long get() = this.inWholeMilliseconds
