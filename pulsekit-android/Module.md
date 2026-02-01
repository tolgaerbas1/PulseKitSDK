# PulseKit Android

Android-specific bindings and integrations for the PulseKit SDK.

## Overview

PulseKit Android provides seamless integration with the Android platform, offering automatic lifecycle management, network monitoring, and Android-specific optimizations. This module builds on the core SDK to provide a truly zero-configuration experience for Android developers.

## Key Features

- **Zero Integration Work**: Automatic setup with ProcessLifecycleOwner
- **Lifecycle-Aware Sessions**: Sessions automatically managed based on app state
- **Activity Monitoring**: Prevents premature session timeouts
- **Network Monitoring**: Real-time connectivity awareness
- **Auto-Initialization**: AndroidX Startup integration
- **Battery Efficient**: Optimized for mobile device constraints

## Architecture

The Android module extends the core SDK with platform-specific capabilities:

### Main Entry Point (`com.pulsekit.android`)
- `PulseKitAndroid`: Android-specific entry point with enhanced features
- `PulseKitInitializer`: AndroidX Startup auto-initialization

### Lifecycle Integration (`com.pulsekit.android.lifecycle`)
- `PulseKitLifecycleObserver`: ProcessLifecycleOwner integration
- `SessionLifecycleListener`: Callbacks for session state changes

### Network Integration (`com.pulsekit.android.network`)
- `NetworkMonitor`: Real-time connectivity monitoring
- `NetworkStatus`: Current network state information

### Storage Integration (`com.pulsekit.android.storage`)
- `AndroidDatabaseDriver`: Android-specific SQLite implementation
- `DatabaseDriverFactory`: Platform-specific driver creation

## Usage

### Zero-Configuration Setup

Simply add one line to your Application class:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // That's it! Everything else is automatic.
        PulseKitAndroid.initialize(this)
    }
}
```

### Custom Configuration

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val config = PulseKitConfig {
            apiKey = "your-api-key"
            enableDebugLogging = BuildConfig.DEBUG
            enableAutoSessionManagement = true
            enableOfflineQueueing = true
            enableDiskPersistence = true
            
            metadata("app_version", BuildConfig.VERSION_NAME)
            metadata("build_type", BuildConfig.BUILD_TYPE)
        }
        
        PulseKitAndroid.initialize(this, config)
    }
}
```

### Event Tracking

```kotlin
// Track events - activity is automatically monitored
PulseKitAndroid.instance.track(
    CustomEvent(
        eventName = "button_clicked",
        metadata = mapOf(
            "button_id" to "signup_button",
            "screen" to "onboarding"
        )
    )
)

// Optional: Manually update activity to prevent timeout
PulseKitAndroid.updateActivity()
```

### Session Monitoring

```kotlin
// Check if app is in foreground
val isForeground = PulseKitAndroid.isAppInForeground()

// Get detailed session information
val sessionInfo = PulseKitAndroid.getCurrentSessionInfo()
if (sessionInfo != null) {
    println("Session ID: ${sessionInfo.sessionId}")
    println("Is active: ${sessionInfo.isForeground}")
    println("Is paused: ${sessionInfo.isPaused}")
    println("Last activity: ${sessionInfo.lastActivityTime}")
}

// Set up session lifecycle callbacks
PulseKitAndroid.setSessionListener(object : SessionLifecycleListener {
    override fun onSessionStarted(sessionId: String) {
        Log.d("PulseKit", "Session started: $sessionId")
    }
    
    override fun onSessionEnded(sessionId: String) {
        Log.d("PulseKit", "Session ended: $sessionId")
    }
    
    override fun onSessionTimedOut(sessionId: String) {
        Log.d("PulseKit", "Session timed out: $sessionId")
    }
})
```

## Automatic Features

### Session Management

The SDK automatically handles session lifecycle:

- **App → Foreground**: New session starts
- **App → Background**: Session pauses (doesn't end immediately)
- **App returns to Foreground**: Session resumes
- **5 minutes inactivity**: Session times out
- **User activity detected**: Timeout timer resets

### Lifecycle Event Tracking

All lifecycle transitions are automatically tracked:

```kotlin
// These events are tracked automatically:
LifecycleEvent(action = FOREGROUND, component = "application")
LifecycleEvent(action = BACKGROUND, component = "application")
SessionEvent(action = START, sessionId = "...")
SessionEvent(action = RESUME, sessionId = "...")
SessionEvent(action = END, sessionId = "...")
```

### Activity Monitoring

The SDK automatically monitors user activity:

- **Event Tracking**: Any `track()` call updates activity timestamp
- **Manual Updates**: Call `PulseKitAndroid.updateActivity()` for additional signals
- **Timeout Prevention**: Activity prevents premature session timeouts

## Auto-Initialization

For truly zero configuration, use AndroidX Startup:

```xml
<!-- In AndroidManifest.xml -->
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false">
    
    <meta-data
        android:name="com.pulsekit.android.startup.PulseKitInitializer"
        android:value="androidx.startup" />
        
    <meta-data
        android:name="com.pulsekit.api_key"
        android:value="your-api-key" />
        
    <meta-data
        android:name="com.pulsekit.enable_debug"
        android:value="true" />
</provider>
```

## Network Monitoring

The SDK monitors network connectivity for optimal performance:

```kotlin
// Get current network status
val networkMonitor = NetworkMonitor.getInstance(context)
val status = networkMonitor.getCurrentStatus()

println("Connected: ${status.isConnected}")
println("Network type: ${status.networkType}")
println("Is metered: ${status.isMetered}")

// Observe network changes
networkMonitor.isConnected.collect { isConnected ->
    Log.d("PulseKit", "Network connected: $isConnected")
}
```

## Performance Optimizations

### Battery Efficiency

- **Structured Concurrency**: Uses coroutines for efficient async operations
- **Lifecycle Awareness**: Pauses operations when app is backgrounded
- **Batch Processing**: Groups operations to minimize wake locks
- **Smart Polling**: Adjusts frequency based on app state

### Memory Management

- **Bounded Queues**: Prevents memory leaks with size limits
- **Automatic Cleanup**: Removes expired events
- **Efficient Storage**: Uses Android's SQLite optimizations
- **Minimal Overhead**: < 1MB additional memory footprint

## Dependencies

- AndroidX Lifecycle: ProcessLifecycleOwner integration
- AndroidX Startup: Auto-initialization support
- AndroidX SQLite: Optimized database operations
- Core SDK: All core functionality

## Permissions Required

```xml
<!-- Required for network access -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Optional: For background processing -->
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

## ProGuard Configuration

The SDK includes ProGuard rules automatically, but if you need custom rules:

```proguard
# Keep PulseKit classes
-keep class com.pulsekit.** { *; }
-keep class com.pulsekit.android.** { *; }

# Keep serialization classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-dontnote kotlinx.serialization.SerializationKt

# Keep SQLite classes
-keep class android.database.sqlite.** { *; }
```

## Testing

The module includes Android-specific tests:

```bash
# Run Android tests
./gradlew :pulsekit-android:test

# Run instrumented tests
./gradlew :pulsekit-android:connectedAndroidTest
```

## Migration from Core SDK

If you're currently using the core SDK directly:

```kotlin
// Before
PulseKit.initialize(context, config)

// After
PulseKitAndroid.initialize(context, config)
```

All existing APIs remain the same, with additional Android-specific features available.

## License

Apache License 2.0 - see LICENSE file for details.
