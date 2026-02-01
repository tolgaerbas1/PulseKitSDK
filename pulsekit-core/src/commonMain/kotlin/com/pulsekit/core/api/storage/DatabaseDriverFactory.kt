package com.pulsekit.core.api.storage

import com.pulsekit.core.api.config.PulseKitConfig

/**
 * Platform-agnostic factory for creating database drivers.
 * 
 * Each platform (JVM, Android, iOS, etc.) provides its own implementation
 * of this factory to create the appropriate database driver.
 */
internal expect class DatabaseDriverFactory() {
    
    /**
     * Create a database driver for the current platform.
     * 
     * @param config The PulseKit configuration
     * @param context Platform-specific context (if needed)
     * @return A platform-specific database driver
     */
    fun createDriver(config: PulseKitConfig, context: Any?): DatabaseDriver
}
