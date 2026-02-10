package com.pulsekit.core.api.config

import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PulseKitConfigTest {

    @Test
    fun defaultConfig_hasExpectedDefaults() {
        val config = PulseKitConfig()
        assertEquals("https://api.pulsekit.dev", config.baseUrl)
        assertEquals(false, config.enableDebugLogging)
        assertEquals(1000, config.maxQueueSize)
        assertEquals(30, config.sessionTimeout.inWholeMinutes)
        assertTrue(config.enableAutoSessionManagement)
        assertTrue(config.enableOfflineQueueing)
        assertNull(config.apiKey)
    }

    @Test
    fun development_hasDebugEnabled() {
        val config = PulseKitConfig.development()
        assertTrue(config.enableDebugLogging)
        assertTrue(config.enableAutoSessionManagement)
        assertTrue(config.enableOfflineQueueing)
    }

    @Test
    fun production_hasDebugDisabled() {
        val config = PulseKitConfig.production()
        assertEquals(false, config.enableDebugLogging)
        assertTrue(config.enableAutoSessionManagement)
        assertTrue(config.enableOfflineQueueing)
    }

    @Test
    fun dsl_buildsConfigWithSetValues() {
        val config = PulseKitConfig {
            apiKey = "test-key"
            enableDebugLogging = true
            sessionTimeout = 15.minutes
            metadata("app", "test")
        }
        assertEquals("test-key", config.apiKey)
        assertTrue(config.enableDebugLogging)
        assertEquals(15, config.sessionTimeout.inWholeMinutes)
        assertEquals(mapOf("app" to "test"), config.globalMetadata)
    }

    @Test
    fun dsl_emptyBlock_usesDefaults() {
        val config = PulseKitConfig { }
        assertEquals("https://api.pulsekit.dev", config.baseUrl)
        assertTrue(config.globalMetadata.isEmpty())
    }
}
