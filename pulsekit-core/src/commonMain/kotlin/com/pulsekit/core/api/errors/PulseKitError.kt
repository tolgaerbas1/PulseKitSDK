package com.pulsekit.core.api.errors

/**
 * Base sealed interface for all PulseKit errors.
 *
 * This ensures type-safe error handling and prevents
 * unexpected error types from being thrown.
 */
public sealed class PulseKitError(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : Throwable(message, cause) {

    public sealed class Event(
        message: String? = null,
        cause: Throwable? = null,
    ) : PulseKitError(message, cause) {

        public class ProcessingFailed(
            message: String? = "Event processing failed",
            cause: Throwable? = null,
        ) : Event(message, cause)

        public class EventTooLarge(
            public val eventSize: Int,
            public val maxSize: Int,
        ) : Event("Event too large: $eventSize > $maxSize")

        public class InvalidEvent(
            message: String,
        ) : Event(message)
    }

    // Diğer hata kategorileri gerektiğinde buraya eklenebilir
}

/**
 * Errors related to SDK initialization and configuration.
 */
public sealed class InitializationError(
    override val message: String,
    override val cause: Throwable? = null,
    val code: String,
) : PulseKitError() {

    public class NotInitialized(
        override val cause: Throwable? = null,
    ) : InitializationError(
        message = "PulseKit has not been initialized. Call PulseKit.initialize() first.",
        cause = cause,
        code = "NOT_INITIALIZED",
    )

    public class AlreadyInitialized(
        override val cause: Throwable? = null,
    ) : InitializationError(
        message = "PulseKit is already initialized. Multiple initialization is not supported.",
        cause = cause,
        code = "ALREADY_INITIALIZED",
    )

    public class InvalidConfiguration(
        override val message: String,
        override val cause: Throwable? = null,
    ) : InitializationError(
        message = message,
        cause = cause,
        code = "INVALID_CONFIGURATION",
    )
}

/**
 * Errors related to event processing and tracking.
 */
public sealed class EventError(
    override val message: String,
    override val cause: Throwable? = null,
    val code: String,
) : PulseKitError() {

    public class EventTooLarge(
        public val eventSize: Int,
        public val maxSize: Int,
        override val cause: Throwable? = null,
    ) : EventError(
        message = "Event size ($eventSize bytes) exceeds maximum allowed size ($maxSize bytes).",
        cause = cause,
        code = "EVENT_TOO_LARGE",
    )

    public class InvalidEvent(
        override val message: String,
        override val cause: Throwable? = null,
    ) : EventError(
        message = message,
        cause = cause,
        code = "INVALID_EVENT",
    )

    public class QueueFull(
        public val currentSize: Int,
        public val maxSize: Int,
        override val cause: Throwable? = null,
    ) : EventError(
        message = "Event queue is full ($currentSize/$maxSize). Events are being dropped.",
        cause = cause,
        code = "QUEUE_FULL",
    )

    public class ProcessingFailed(
        override val message: String,
        override val cause: Throwable? = null,
    ) : EventError(
        message = "Failed to process event: $message",
        cause = cause,
        code = "PROCESSING_FAILED",
    )
}

/**
 * Errors related to network operations.
 */
public sealed class NetworkError(
    override val message: String,
    override val cause: Throwable? = null,
    val code: String,
) : PulseKitError() {

    public class NoConnection(
        override val cause: Throwable? = null,
    ) : NetworkError(
        message = "No network connection available.",
        cause = cause,
        code = "NO_CONNECTION",
    )

    public class ServerError(
        public val statusCode: Int,
        override val message: String,
        override val cause: Throwable? = null,
    ) : NetworkError(
        message = "Server error ($statusCode): $message",
        cause = cause,
        code = "SERVER_ERROR",
    )

    public class Timeout(
        public val timeoutMs: Long,
        override val cause: Throwable? = null,
    ) : NetworkError(
        message = "Network operation timed out after ${timeoutMs}ms.",
        cause = cause,
        code = "TIMEOUT",
    )

    public class AuthenticationFailed(
        override val cause: Throwable? = null,
    ) : NetworkError(
        message = "Authentication failed. Check your API key configuration.",
        cause = cause,
        code = "AUTHENTICATION_FAILED",
    )
}

/**
 * Errors related to storage and persistence.
 */
public sealed class StorageError(
    override val message: String,
    override val cause: Throwable? = null,
    val code: String,
) : PulseKitError() {

    public class DiskFull(
        public val availableBytes: Long,
        override val cause: Throwable? = null,
    ) : StorageError(
        message = "Insufficient disk space ($availableBytes bytes available).",
        cause = cause,
        code = "DISK_FULL",
    )

    public class CorruptedData(
        override val message: String,
        override val cause: Throwable? = null,
    ) : StorageError(
        message = "Corrupted data detected: $message",
        cause = cause,
        code = "CORRUPTED_DATA",
    )

    public class AccessDenied(
        override val cause: Throwable? = null,
    ) : StorageError(
        message = "Storage access denied. Check file permissions.",
        cause = cause,
        code = "ACCESS_DENIED",
    )
}

/**
 * Errors related to session management.
 */
public sealed class SessionError(
    override val message: String,
    override val cause: Throwable? = null,
    val code: String,
) : PulseKitError() {

    public class SessionExpired(
        public val sessionId: String,
        override val cause: Throwable? = null,
    ) : SessionError(
        message = "Session $sessionId has expired.",
        cause = cause,
        code = "SESSION_EXPIRED",
    )

    public class InvalidSession(
        public val sessionId: String,
        override val cause: Throwable? = null,
    ) : SessionError(
        message = "Invalid session ID: $sessionId",
        cause = cause,
        code = "INVALID_SESSION",
    )
}

/**
 * Catch-all for unexpected errors.
 */
public class UnknownError(
    override val message: String,
    override val cause: Throwable? = null,
) : PulseKitError() {

    val code: String = "UNKNOWN_ERROR"
}
