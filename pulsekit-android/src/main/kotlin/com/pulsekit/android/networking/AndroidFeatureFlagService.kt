package com.pulsekit.android.networking

import com.pulsekit.core.api.flags.FeatureFlagManager
import com.pulsekit.core.api.networking.FeatureFlagService
import com.pulsekit.core.api.networking.NetworkClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Android-specific implementation of feature flag service.
 * 
 * This service handles network requests for feature flags using Android's
 * networking capabilities and integrates with the Android lifecycle.
 */
internal class AndroidFeatureFlagService(
    private val networkClient: NetworkClient,
    private val flagManager: FeatureFlagManager,
    private val scope: CoroutineScope
) : FeatureFlagService(networkClient, flagManager, scope) {
    
    override suspend fun fetchFeatureFlags(): Result<com.pulsekit.core.api.networking.FeatureFlagResponse> {
        return try {
            // Use Android-specific networking
            val response = networkClient.get("/api/v1/feature-flags")
            
            if (response.isSuccess) {
                val flagResponse = parseFlagResponse(response.body ?: "")
                updateFlagManager(flagResponse)
                Result.success(flagResponse)
            } else {
                val error = com.pulsekit.core.api.networking.NetworkError(
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
    private fun parseFlagResponse(jsonBody: String): com.pulsekit.core.api.networking.FeatureFlagResponse {
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        
        val jsonObject = json.decodeFromString<kotlinx.serialization.json.JsonObject>(jsonBody)
        
        val flags = mutableMapOf<String, com.pulsekit.core.api.flags.FlagValue>()
        
        jsonObject["flags"]?.let { flagsArray ->
            if (flagsArray is kotlinx.serialization.json.JsonObject) {
                flagsArray.forEach { (key, element) ->
                    val flagValue = parseFlagValue(element)
                    if (flagValue != null) {
                        flags[key] = flagValue
                    }
                }
            }
        }
        
        return com.pulsekit.core.api.networking.FeatureFlagResponse(
            flags = flags,
            timestamp = jsonObject["timestamp"]?.toString()?.toLongOrNull() ?: System.currentTimeMillis(),
            version = jsonObject["version"]?.toString() ?: "unknown"
        )
    }
    
    /**
     * Parse individual flag value from JSON element.
     */
    private fun parseFlagValue(element: kotlinx.serialization.json.JsonElement): com.pulsekit.core.api.flags.FlagValue? {
        return when (element) {
            is kotlinx.serialization.json.JsonPrimitive -> {
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
    private fun updateFlagManager(response: com.pulsekit.core.api.networking.FeatureFlagResponse) {
        flagManager.updateServerFlags(response.flags)
    }
    
    /**
     * Start periodic flag fetching with Android-specific optimizations.
     */
    override fun startPeriodicFetching(intervalMs: Long = 300000L) {
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
