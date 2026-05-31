package com.pulsekit.android.networking

import com.pulsekit.core.api.networking.NetworkClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

/**
 * Android-specific implementation of feature flag service.
 *
 * This service handles network requests for feature flags using Android's
 * networking capabilities and integrates with the Android lifecycle.
 */
internal class AndroidFeatureFlagService(
    private val networkClient: NetworkClient,
    private val scope: CoroutineScope,
) {

    suspend fun fetchFeatureFlags(): Result<FeatureFlagResponse> {
        return try {
            val responseBody = networkClient.get("/api/v1/feature-flags")
            val flagResponse = parseFlagResponse(responseBody)
            Result.success(flagResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parse feature flag response from JSON.
     */
    private fun parseFlagResponse(jsonBody: String): FeatureFlagResponse {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        val jsonObject = json.decodeFromString<JsonObject>(jsonBody)

        val flags = mutableMapOf<String, com.pulsekit.core.api.flags.FlagValue>()

        val flagsObject = jsonObject["flags"] as? JsonObject
        flagsObject?.forEach { (key: String, element: JsonElement) ->
            val flagValue = parseFlagValue(element)
            if (flagValue != null) {
                flags[key] = flagValue
            }
        }

        return FeatureFlagResponse(
            flags = flags,
            timestamp = jsonObject["timestamp"]?.toString()?.toLongOrNull() ?: System.currentTimeMillis(),
            version = jsonObject["version"]?.toString() ?: "unknown",
        )
    }

    /**
     * Parse individual flag value from JSON element.
     */
    private fun parseFlagValue(element: JsonElement): com.pulsekit.core.api.flags.FlagValue? {
        return when (element) {
            is JsonPrimitive -> {
                when {
                    element.isString -> com.pulsekit.core.api.flags.FlagValue.StringValue(element.content)
                    element.booleanOrNull != null -> com.pulsekit.core.api.flags.FlagValue.BooleanValue(element.boolean)
                    element.longOrNull != null -> com.pulsekit.core.api.flags.FlagValue.IntegerValue(element.long)
                    element.doubleOrNull != null -> com.pulsekit.core.api.flags.FlagValue.DoubleValue(element.double)
                    else -> null
                }
            }
            else -> null
        }
    }

    /**
     * Update flag manager with server values.
     */
    /**
     * Start periodic flag fetching with Android-specific optimizations.
     */
    fun startPeriodicFetching(intervalMs: Long = 300000L) {
        scope.launch {
            while (true) {
                try {
                    // Only fetch when app is in foreground
                    if (com.pulsekit.android.lifecycle.PulseKitLifecycleObserver.isAppInForeground()) {
                        fetchFeatureFlags()
                    }
                    kotlinx.coroutines.delay(intervalMs)
                } catch (e: Exception) {
                    // Log error but continue fetching
                    kotlinx.coroutines.delay(intervalMs)
                }
            }
        }
    }
}

internal data class FeatureFlagResponse(
    val flags: Map<String, com.pulsekit.core.api.flags.FlagValue>,
    val timestamp: Long,
    val version: String,
)
