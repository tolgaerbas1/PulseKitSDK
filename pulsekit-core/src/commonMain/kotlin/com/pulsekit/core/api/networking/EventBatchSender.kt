package com.pulsekit.core.api.networking

/**
 * Sends a batch of events (as JSON payload) to the backend.
 * Implemented by the platform layer (e.g. Android with HTTP client).
 */
public interface EventBatchSender {
    /**
     * Send the batch payload to the network.
     * @param jsonPayload JSON-serialized batch of events
     * @return true if send succeeded, false otherwise (caller may retry or mark failed)
     */
    public suspend fun sendBatch(jsonPayload: String): Boolean
}
