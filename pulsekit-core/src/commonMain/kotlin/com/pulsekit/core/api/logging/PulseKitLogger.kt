package com.pulsekit.core.api.logging

/**
 * Central logger for PulseKit SDK. Default implementation uses [println].
 * On Android, call [init] with [android.util.Log] in [PulseKitAndroid.initialize] to use tagged log.
 */
public object PulseKitLogger {
    private var logFn: (String, String) -> Unit = { tag, message ->
        println("$tag: $message")
    }

    /**
     * Set the log implementation (e.g. Android Log.d). Call from platform code (e.g. PulseKitAndroid.initialize).
     */
    public fun init(log: (tag: String, message: String) -> Unit) {
        logFn = log
    }

    /**
     * Log a debug message. Call only when [com.pulsekit.core.api.config.PulseKitConfig.enableDebugLogging] is true at call sites.
     */
    public fun log(tag: String, message: String) {
        logFn(tag, message)
    }
}
