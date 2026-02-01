package com.pulsekit.core.api.networking

import com.pulsekit.core.api.flags.FeatureFlag
import com.pulsekit.core.api.flags.FlagValue
import com.pulsekit.core.api.flags.FeatureFlagManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.double

/**
 * Service for fetching feature flags from the server.
 * 
 * This service handles network requests for feature flags and updates
 * the FeatureFlagManager with server values.
 */
internal class FeatureFlagService(
    private val networkClient: NetworkClient,
    private val flagManager: FeatureFlagManager,
    private val scope: CoroutineScope
) {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Fetch feature flags from the server.
     */
    suspend fun fetchFeatureFlags(): Result<FeatureFlagResponse> {
        return try {
            val response = networkClient.get("/api/v1/feature-flags")
            
            if (response.isSuccess) {
                val flagResponse = parseFlagResponse(response.body ?: "")
                updateFlagManager(flagResponse)
                Result.success(flagResponse)
            } else {
                val error = NetworkError(
                    statusCode = response.statusCode,
                    message = response.statusMessage ?: "Unknown error"
                )
                flagManager.handleFetchFailure(error)
                Result.failure(error)
            }
        } catch (e: Exception) {
            flagManager.handleFetchFailure(e)
            Result.failure(e)
        }
    }
    
    /**
     * Parse feature flag response from JSON.
     */
    private fun parseFlagResponse(jsonBody: String): FeatureFlagResponse {
        val jsonObject = json.decodeFromString<JsonObject>(jsonBody)
        
        val flags = mutableMapOf<String, FlagValue>()
        
        jsonObject["flags"]?.let { flagsArray ->
            if (flagsArray is JsonObject) {
                flagsArray.forEach { (key, element) ->
                    val flagValue = parseFlagValue(element)
                    if (flagValue != null) {
                        flags[key] = flagValue
                    }
                }
            }
        }
        
        return FeatureFlagResponse(
            flags = flags,
            timestamp = jsonObject["timestamp"]?.toString()?.toLongOrNull() ?: System.currentTimeMillis(),
            version = jsonObject["version"]?.toString() ?: "unknown"
        )
    }
    
    /**
     * Parse individual flag value from JSON element.
     */
    private fun parseFlagValue(element: kotlinx.serialization.json.JsonElement): FlagValue? {
        return when (element) {
            is kotlinx.serialization.json.JsonPrimitive -> {
                when {
                    element.isString -> FlagValue.StringValue(element.content)
                    element.booleanOrNull != null -> FlagValue.BooleanValue(element.boolean)
                    element.longOrNull != null -> FlagValue.IntegerValue(element.long)
                    element.doubleOrNull != null -> FlagValue.DoubleValue(element.double)
                    else -> null
                }
            }
            else -> null
        }
    }
    
    /**
     * Update flag manager with server values.
     */
    private fun updateFlagManager(response: FeatureFlagResponse) {
        flagManager.updateServerFlags(response.flags)
    }
    
    /**
     * Start periodic flag fetching.
     */
    fun startPeriodicFetching(intervalMs: Long = 300000L) { // 5 minutes default
        scope.launch {
            while (true) {
                try {
                    fetchFeatureFlags()
                    kotlinx.coroutines.delay(intervalMs)
                } catch (e: Exception) {
                    // Log error but continue fetching
                    kotlinx.coroutines.delay(intervalMs)
                }
            }
        }
    }
}

/**
 * Response from the feature flag endpoint.
 */
@Serializable
internal data class FeatureFlagResponse(
    val flags: Map<String, FlagValue>,
    val timestamp: Long,
    val version: String
)

/**
 * Network error for feature flag operations.
 */
internal class NetworkError(
    val statusCode: Int,
    override val message: String
) : Exception(message)
