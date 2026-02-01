package com.pulsekit.core.api.storage

import com.pulsekit.core.api.config.PulseKitConfig
import android.content.Context

/**
 * Android implementation of DatabaseDriverFactory.
 */
internal actual class DatabaseDriverFactory {
    
    actual fun createDriver(config: PulseKitConfig, context: Any?): DatabaseDriver {
        val androidContext = context as? Context 
            ?: throw IllegalArgumentException("Android Context is required for database driver")
        
        return AndroidDatabaseDriver(androidContext)
    }
}
