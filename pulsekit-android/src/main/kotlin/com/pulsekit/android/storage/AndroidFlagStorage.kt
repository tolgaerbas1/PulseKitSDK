package com.pulsekit.android.storage

import android.content.Context
import com.pulsekit.core.api.flags.PlatformFlagStorage
import java.io.File

/**
 * Android implementation of PlatformFlagStorage.
 * 
 * Uses SharedPreferences for simple flag storage.
 */
internal class AndroidFlagStorage(
    private val context: Context
) : PlatformFlagStorage {
    
    private val prefs = context.getSharedPreferences("pulsekit_feature_flags", Context.MODE_PRIVATE)
    
    override suspend fun save(key: String, data: String) {
        prefs.edit().putString(key, data).apply()
    }
    
    override suspend fun load(key: String): String? {
        return prefs.getString(key, null)
    }
    
    override suspend fun clear(key: String) {
        prefs.edit().remove(key).apply()
    }
}

/**
 * File-based Android implementation of PlatformFlagStorage.
 * 
 * Uses internal storage for larger flag data.
 */
internal class AndroidFileFlagStorage(
    private val context: Context
) : PlatformFlagStorage {
    
    private val flagsDir = File(context.filesDir, "pulsekit_flags")
    
    init {
        flagsDir.mkdirs()
    }
    
    override suspend fun save(key: String, data: String) {
        val file = File(flagsDir, key)
        file.writeText(data)
    }
    
    override suspend fun load(key: String): String? {
        val file = File(flagsDir, key)
        return if (file.exists()) {
            file.readText()
        } else {
            null
        }
    }
    
    override suspend fun clear(key: String) {
        val file = File(flagsDir, key)
        if (file.exists()) {
            file.delete()
        }
    }
}
