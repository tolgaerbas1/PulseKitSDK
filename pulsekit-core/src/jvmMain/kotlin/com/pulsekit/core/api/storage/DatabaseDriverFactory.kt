package com.pulsekit.core.api.storage

import com.pulsekit.core.api.config.PulseKitConfig
import java.io.File

/**
 * JVM implementation of DatabaseDriverFactory.
 */
internal actual class DatabaseDriverFactory {

    actual fun createDriver(config: PulseKitConfig, context: Any?): DatabaseDriver {
        val databasePath = getDatabasePath(context)
        return SqliteDatabaseDriver(databasePath)
    }

    private fun getDatabasePath(context: Any?): String {
        // Try to get a suitable database path
        return when (context) {
            is String -> {
                // If context is a string path, use it
                val dir = File(context)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                File(dir, DatabaseSchema.DATABASE_NAME).absolutePath
            }
            else -> {
                // Default to user home directory
                val userHome = System.getProperty("user.home")
                val pulsekitDir = File(userHome, ".pulsekit")
                if (!pulsekitDir.exists()) {
                    pulsekitDir.mkdirs()
                }
                File(pulsekitDir, DatabaseSchema.DATABASE_NAME).absolutePath
            }
        }
    }
}
