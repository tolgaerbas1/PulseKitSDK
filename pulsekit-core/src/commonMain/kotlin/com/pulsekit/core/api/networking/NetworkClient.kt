package com.pulsekit.core.api.networking

/**
 * Network client for making HTTP requests.
 */
interface NetworkClient {
    suspend fun get(url: String): String
    suspend fun post(url: String, body: String): String
}
