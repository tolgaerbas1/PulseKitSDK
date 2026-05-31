package com.pulsekit.core.api

import com.pulsekit.core.api.config.PulseKitConfig
import com.pulsekit.core.api.events.PulseEvent
import com.pulsekit.core.api.networking.EventBatchSender
import com.pulsekit.core.api.session.SessionManager
import kotlinx.coroutines.CoroutineScope

/**
 * Main entry point for PulseKit SDK.
 *
 * This is the single entry point for initializing and accessing PulseKit functionality.
 * The SDK is designed to be lifecycle-aware and offline-first.
 *
 * @see PulseKitConfig for configuration options
 * @see PulseEvent for event tracking
 * @see SessionManager for session management
 */
public object PulseKit {

    private var _instance: PulseKitInstance? = null

    /**
     * The initialized SDK instance, or null if not yet initialized.
     *
     * @throws IllegalStateException if accessed before initialization
     */
    public val instance: PulseKitInstance
        get() = _instance ?: throw IllegalStateException(
            "PulseKit must be initialized before use. Call PulseKit.initialize() first.",
        )

    /**
     * Check if PulseKit has been initialized.
     */
    public val isInitialized: Boolean
        get() = _instance != null

    /**
     * Initialize PulseKit with the specified configuration.
     *
     * This method should be called once, typically in your Application's onCreate().
     * Multiple calls will be ignored after the first successful initialization.
     *
     * @param config The configuration for PulseKit. Use PulseKitConfig { } for DSL-style configuration.
     * @param scope Optional coroutine scope for SDK operations. If null, a default scope will be created.
     * @return The initialized PulseKitInstance
     */
    public fun initialize(
        config: PulseKitConfig = PulseKitConfig(),
        scope: CoroutineScope? = null,
        batchSender: EventBatchSender? = null,
    ): PulseKitInstance {
        if (_instance == null) {
            _instance = PulseKitInstance(config, scope, batchSender)
        }
        return _instance!!
    }

    /**
     * Reset the SDK instance.
     *
     * This is primarily intended for testing purposes.
     * Calling this in production code will require re-initialization.
     */
    @JvmSynthetic
    internal fun reset() {
        _instance?.shutdown()
        _instance = null
    }
}
