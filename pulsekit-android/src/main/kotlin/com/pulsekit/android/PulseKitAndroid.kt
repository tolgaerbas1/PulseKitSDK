package com.pulsekit.android

import android.content.Context
import android.util.Log
import com.pulsekit.android.lifecycle.PulseKitLifecycleObserver
import com.pulsekit.android.lifecycle.SessionLifecycleListener
import com.pulsekit.android.network.NetworkMonitor
import com.pulsekit.android.networking.AndroidEventBatchSender
import com.pulsekit.android.networking.AndroidNetworkClient
import com.pulsekit.android.storage.AndroidFileFlagStorage
import com.pulsekit.core.api.PulseKit
import com.pulsekit.core.api.config.PulseKitConfig
import com.pulsekit.core.api.events.ErrorEvent
import com.pulsekit.core.api.events.ErrorType
import com.pulsekit.core.api.flags.DiskFlagStorage
import com.pulsekit.core.api.flags.FlagPersistence
import com.pulsekit.core.api.flags.InMemoryFlagStorage
import com.pulsekit.core.api.flags.PulseKitFeatureFlags
import com.pulsekit.core.api.logging.PulseKitLogger
import com.pulsekit.core.api.storage.createDatabaseDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Android-specific entry point for PulseKit SDK with zero integration work.
 *
 * This provides Android-specific initialization and automatic lifecycle integration.
 * The SDK automatically handles session management, lifecycle tracking, and
 * offline-first event persistence without any additional setup required.
 *
 * Key features:
 * - Automatic session management based on app lifecycle
 * - Zero configuration required for basic usage
 * - Comprehensive lifecycle event tracking
 * - Session timeout handling
 * - Activity monitoring to prevent premature timeouts
 * - Server-driven feature flags for remote behavior control
 */
public object PulseKitAndroid {
    private var integrationScope: CoroutineScope? = null
    private var networkMonitoringJob: Job? = null
    private var crashReportingInstalled: Boolean = false
    private var previousCrashHandler: Thread.UncaughtExceptionHandler? = null

    /**
     * Initialize PulseKit with Android-specific features and zero integration work.
     *
     * This method automatically sets up:
     * - Lifecycle observers for automatic session management
     * - Session timeout monitoring
     * - Activity tracking
     * - Comprehensive event tracking
     * - Server-driven feature flags
     * - Network connectivity monitoring
     *
     * Simply call this in your Application's onCreate() and everything else is handled.
     *
     * @param context Application context
     * @param config The configuration for PulseKit
     * @param enableLifecycleObserver Whether to enable automatic lifecycle integration
     * @return The initialized PulseKitInstance
     */
    public fun initialize(
        context: Context,
        config: PulseKitConfig = PulseKitConfig(),
        enableLifecycleObserver: Boolean = true,
    ) {
        // Use Android Log for SDK logging when running on Android
        PulseKitLogger.init { tag, message -> Log.d(tag, message) }
        // Initialize core SDK with Android batch sender for network upload
        val batchSender = AndroidEventBatchSender(config)
        val instance = PulseKit.initialize(config, CoroutineScope(Dispatchers.Default), batchSender)
        // Set up Android-specific integrations
        if (enableLifecycleObserver && config.enableAutoSessionManagement) {
            PulseKitLifecycleObserver.initialize(context, instance)
        }

        // Initialize feature flag system with networking and persistence
        initializeFeatureFlags(context, config, instance)

        // Initialize disk-backed event persistence
        if (config.enableDiskPersistence) {
            instance.configureEventPersistence(createDatabaseDriver(context))
        }
        // Network connectivity monitoring: flush when back online
        setupNetworkConnectivityMonitoring(context, instance)
        // Opt-in crash reporting: track uncaught exceptions as fatal ErrorEvents
        if (config.enableCrashReporting) {
            setupCrashReporting(instance)
        }
    }

    /**
     * Observes network connectivity and flushes event queue when connection is restored.
     */
    private fun setupNetworkConnectivityMonitoring(
        context: Context,
        instance: com.pulsekit.core.api.PulseKitInstance,
    ) {
        if (networkMonitoringJob != null) return

        val monitor = runCatching { NetworkMonitor.getInstance(context) }.getOrElse { error ->
            if (instance.config.enableDebugLogging) {
                PulseKitLogger.log("PulseKit", "Network monitoring unavailable: ${error.message}")
            }
            return
        }

        networkMonitoringJob = getIntegrationScope().launch {
            runCatching {
                var wasConnected = monitor.isConnected.first()
                monitor.isConnected.collect { isConnected ->
                    if (isConnected && !wasConnected) {
                        instance.flush()
                    }
                    wasConnected = isConnected
                }
            }.onFailure { error ->
                if (instance.config.enableDebugLogging) {
                    PulseKitLogger.log("PulseKit", "Network monitoring stopped: ${error.message}")
                }
            }
        }
    }

    /**
     * Sets default UncaughtExceptionHandler to track fatal errors as ErrorEvents,
     * then delegates to the previous handler.
     */
    private fun setupCrashReporting(instance: com.pulsekit.core.api.PulseKitInstance) {
        if (crashReportingInstalled) return

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        previousCrashHandler = defaultHandler
        crashReportingInstalled = true
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                instance.track(
                    ErrorEvent(
                        errorType = ErrorType.RUNTIME,
                        message = throwable.message ?: "Uncaught exception",
                        stackTrace = throwable.stackTraceToString(),
                        isFatal = true,
                        metadata = mapOf(
                            "thread" to (thread.name ?: "unknown"),
                            "exception" to (throwable.javaClass.name),
                        ),
                    ),
                )
            } catch (_: Exception) {
                // Never let SDK break the app
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Initialize the feature flag system with Android-specific networking and persistence.
     *
     * Creates:
     * 1. Platform-appropriate flag storage (disk or in-memory based on config)
     * 2. A [FlagPersistence] layer backed by that storage
     * 3. An [AndroidNetworkClient] for server communication
     * 4. Wires everything together via [PulseKitInstance.configureFeatureFlags]
     */
    private fun initializeFeatureFlags(
        context: Context,
        config: PulseKitConfig,
        instance: com.pulsekit.core.api.PulseKitInstance,
    ) {
        val flagStorage = if (config.enableDiskPersistence) {
            val platformStorage = AndroidFileFlagStorage(context)
            DiskFlagStorage(platformStorage)
        } else {
            InMemoryFlagStorage()
        }

        val flagPersistence = FlagPersistence(storage = flagStorage)

        val networkClient = AndroidNetworkClient(config)

        instance.configureFeatureFlags(networkClient, flagPersistence)
    }

    /**
     * Get the Android-specific context if available.
     *
     * @return Application context, or null if not initialized
     */
    public fun getContext(): Context? {
        return PulseKitLifecycleObserver.getContext()
    }

    /**
     * Check if PulseKit has been initialized with Android support.
     */
    public val isInitialized: Boolean
        get() = PulseKit.isInitialized

    /**
     * Get the underlying PulseKit instance.
     */
    public val instance: com.pulsekit.core.api.PulseKitInstance
        get() = PulseKit.instance

    /**
     * Check if the app is currently in foreground.
     *
     * @return true if app is in foreground, false otherwise
     */
    public fun isAppInForeground(): Boolean {
        return PulseKitLifecycleObserver.isAppInForeground()
    }

    /**
     * Get current session information.
     *
     * @return Current session info, or null if no active session
     */
    public fun getCurrentSessionInfo(): com.pulsekit.android.lifecycle.SessionInfo? {
        return PulseKitLifecycleObserver.getCurrentSessionInfo()
    }

    /**
     * Update activity timestamp to prevent session timeout.
     *
     * Call this when user activity is detected (button clicks, touches, etc.)
     * to ensure the session doesn't timeout due to inactivity.
     *
     * This is optional - the SDK automatically tracks activity during
     * event tracking, but you can call this manually for additional
     * activity signals.
     */
    public fun updateActivity() {
        PulseKitLifecycleObserver.updateActivity()
    }

    /**
     * Set a listener for detailed session lifecycle events.
     *
     * This allows you to monitor session state changes and implement
     * custom logic based on session lifecycle.
     *
     * @param listener The session lifecycle listener, or null to remove
     */
    public fun setSessionListener(listener: SessionLifecycleListener?) {
        PulseKitLifecycleObserver.setSessionListener(listener)
    }

    /**
     * Force cleanup of the lifecycle observer.
     *
     * This is typically only needed for testing or when manually
     * managing the SDK lifecycle. In normal usage, cleanup is handled
     * automatically.
     */
    public fun cleanup() {
        PulseKitLifecycleObserver.cleanup()
        networkMonitoringJob?.cancel()
        networkMonitoringJob = null
        NetworkMonitor.cleanup()
        integrationScope?.cancel()
        integrationScope = null
        if (crashReportingInstalled) {
            Thread.setDefaultUncaughtExceptionHandler(previousCrashHandler)
            previousCrashHandler = null
            crashReportingInstalled = false
        }
    }

    private fun getIntegrationScope(): CoroutineScope {
        return integrationScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default).also {
            integrationScope = it
        }
    }

    /**
     * Get current feature flag values for debugging.
     *
     * This method is primarily for debugging and monitoring.
     * The actual flag values are used internally by the SDK.
     *
     * @return Map of flag keys to their current values
     */
    public fun getFeatureFlagValues(): Map<String, Any> {
        return try {
            val instance = PulseKit.instance
            PulseKitFeatureFlags.ALL_FLAGS.associate { flag ->
                val value = when (flag.type) {
                    com.pulsekit.core.api.flags.FlagType.BOOLEAN -> instance.getBooleanFlag(flag)
                    com.pulsekit.core.api.flags.FlagType.INTEGER -> instance.getIntegerFlag(flag)
                    com.pulsekit.core.api.flags.FlagType.DOUBLE -> instance.getDoubleFlag(flag)
                    com.pulsekit.core.api.flags.FlagType.STRING -> instance.getStringFlag(flag)
                }
                flag.key to value
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Check if a specific feature flag is enabled.
     *
     * This method is primarily for debugging and monitoring.
     * The actual flag values are used internally by the SDK.
     *
     * @param flag The feature flag to check
     * @return Current value of the flag
     */
}
