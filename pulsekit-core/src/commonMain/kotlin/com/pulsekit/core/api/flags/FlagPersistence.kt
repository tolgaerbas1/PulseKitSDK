package com.pulsekit.core.api.flags

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persistence layer for feature flags.
 * 
 * This handles optional disk persistence of feature flag values
 * to maintain state across app restarts.
 */
class FlagPersistence private constructor(
    private val scope: CoroutineScope,
    private val storage: FlagStorage,
    @Suppress("UNUSED_PARAMETER") private val _privateInit: Unit
) {

    constructor(
        scope: CoroutineScope,
        storage: FlagStorage
    ) : this(
        scope = scope,
        storage = storage,
        _privateInit = Unit
    )
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Save feature flags to persistent storage.
     */
    suspend fun saveFlags(flags: Map<String, FlagValue>) {
        scope.launch(Dispatchers.IO) {
            try {
                val serialized = json.encodeToString(flags)
                storage.save(serialized)
            } catch (e: Exception) {
                // Log error but don't crash
                println("Failed to save feature flags: ${e.message}")
            }
        }
    }
    
    /**
     * Load feature flags from persistent storage.
     */
    suspend fun loadFlags(): Map<String, FlagValue>? {
        return try {
            val serialized = storage.load()
            if (serialized != null) {
                json.decodeFromString<Map<String, FlagValue>>(serialized)
            } else {
                null
            }
        } catch (e: Exception) {
            // Log error but don't crash
            println("Failed to load feature flags: ${e.message}")
            null
        }
    }
    
    /**
     * Clear persisted feature flags.
     */
    suspend fun clearFlags() {
        scope.launch(Dispatchers.IO) {
            try {
                storage.clear()
            } catch (e: Exception) {
                // Log error but don't crash
                println("Failed to clear feature flags: ${e.message}")
            }
        }
    }
}

/**
 * Storage abstraction for feature flags.
 */
interface FlagStorage {
    suspend fun save(data: String)
    suspend fun load(): String?
    suspend fun clear()
}

/**
 * In-memory implementation of FlagStorage.
 * 
 * This is used when disk persistence is disabled.
 */
class InMemoryFlagStorage : FlagStorage {
    
    private var data: String? = null
    
    override suspend fun save(data: String) {
        this.data = data
    }
    
    override suspend fun load(): String? {
        return data
    }
    
    override suspend fun clear() {
        data = null
    }
}

/**
 * Disk-based implementation of FlagStorage.
 * 
 * This uses platform-specific storage implementations.
 */
class DiskFlagStorage(
    private val platformStorage: PlatformFlagStorage
) : FlagStorage {
    
    override suspend fun save(data: String) {
        platformStorage.save("feature_flags.json", data)
    }
    
    override suspend fun load(): String? {
        return platformStorage.load("feature_flags.json")
    }
    
    override suspend fun clear() {
        platformStorage.clear("feature_flags.json")
    }
}

/**
 * Platform-specific storage abstraction.
 */
interface PlatformFlagStorage {
    suspend fun save(key: String, data: String)
    suspend fun load(key: String): String?
    suspend fun clear(key: String)
}
