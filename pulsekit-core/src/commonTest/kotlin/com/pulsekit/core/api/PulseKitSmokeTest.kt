package com.pulsekit.core.api

import com.pulsekit.core.api.config.PulseKitConfig
import com.pulsekit.core.api.events.CustomEvent
import com.pulsekit.core.api.events.PulseKitStatus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Smoke test: SDK init + one event track flow.
 * Validates that initialize + track runs without throwing and SDK reports READY.
 */
class PulseKitSmokeTest {

    @Test
    fun initialize_andTrackCustomEvent_sdkReady() = runTest {
        val scope = TestScope(StandardTestDispatcher())
        val config = PulseKitConfig {
            apiKey = "smoke-test-key"
            enableDebugLogging = false
        }
        try {
            val instance = PulseKit.initialize(config, scope)
            instance.track(
                CustomEvent(
                    eventName = "smoke_test",
                    metadata = mapOf("source" to "PulseKitSmokeTest"),
                ),
            )
            assertEquals(PulseKitStatus.READY, instance.getStatus())
        } finally {
            PulseKit.reset()
        }
    }
}
