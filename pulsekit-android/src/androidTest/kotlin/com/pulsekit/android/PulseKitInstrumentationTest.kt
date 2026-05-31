package com.pulsekit.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pulsekit.core.api.config.PulseKitConfig
import com.pulsekit.core.api.events.CustomEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test for PulseKitAndroid.
 * Runs on device/emulator with real Android context.
 */
@RunWith(AndroidJUnit4::class)
class PulseKitInstrumentationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun initialize_andTrackEvent_sdkReady() {
        PulseKitAndroid.initialize(
            context,
            PulseKitConfig {
                apiKey = "instrumentation-test-key"
                enableDebugLogging = false
                enableDiskPersistence = false
            },
            enableLifecycleObserver = false,
        )
        assertTrue(PulseKitAndroid.isInitialized)
        PulseKitAndroid.instance.track(
            CustomEvent(
                eventName = "instrumentation_test",
                metadata = mapOf("source" to "PulseKitInstrumentationTest"),
            ),
        )
        assertEquals(
            com.pulsekit.core.api.events.PulseKitStatus.READY,
            PulseKitAndroid.instance.getStatus(),
        )
    }
}
