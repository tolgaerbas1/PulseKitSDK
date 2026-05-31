package com.pulsekit.android

import android.content.Context
import com.pulsekit.core.api.config.PulseKitConfig
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * Unit tests for PulseKitAndroid.
 *
 * Uses mock Context and disables lifecycle observer and disk persistence
 * so tests run on JVM without Android runtime.
 */
class PulseKitAndroidTest {

    @Test
    fun initialize_withMockContext_setsInitialized() {
        val context = mock(Context::class.java)
        PulseKitAndroid.initialize(
            context,
            PulseKitConfig {
                apiKey = "test-key"
                enableDiskPersistence = false
            },
            enableLifecycleObserver = false,
        )
        assertTrue(PulseKitAndroid.isInitialized)
    }

    @Test
    fun initialize_instance_doesNotThrow() {
        val context = mock(Context::class.java)
        PulseKitAndroid.initialize(
            context,
            PulseKitConfig {
                apiKey = "test"
                enableDiskPersistence = false
            },
            enableLifecycleObserver = false,
        )
        assertNotNull(PulseKitAndroid.instance)
    }
}
