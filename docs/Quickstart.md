# PulseKit Quick Start Guide

This guide will get you up and running with PulseKit in just a few minutes.

## Installation

### 1. Add the Dependency

Add the PulseKit Android library to your app's `build.gradle` file:

```gradle
dependencies {
    implementation("com.pulsekit:pulsekit-android:0.1.0")
}
```

### 2. Sync Your Project

Sync your project with Gradle files to download the dependency.

## Basic Setup

### 1. Initialize PulseKit

The best place to initialize PulseKit is in your `Application` class:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize PulseKit with basic configuration
        PulseKitAndroid.initialize(this, PulseKitConfig {
            apiKey = "your-api-key" // Production: use BuildConfig or env, do not hardcode
            enableDebugLogging = BuildConfig.DEBUG
        })
    }
}
```

Don't forget to register your Application class in `AndroidManifest.xml`:

```xml
<application
    android:name=".MyApplication"
    ...>
</application>
```

### 2. Add Required Permissions

PulseKit requires internet access for sending events. Add these permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Your First Event

Now you're ready to track your first event! Here's how to track a simple button click:

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val myButton = findViewById<Button>(R.id.my_button)
        myButton.setOnClickListener {
            // Track the button click
            PulseKitAndroid.instance.track(
                CustomEvent(
                    eventName = "button_clicked",
                    metadata = mapOf(
                        "button_id" to "my_button",
                        "screen" to "MainActivity"
                    )
                )
            )
            
            // Your button logic here
        }
    }
}
```

That's it! You're now tracking events with PulseKit.

## Common Event Types

### Custom Events

Perfect for tracking user actions specific to your app:

```kotlin
PulseKitAndroid.instance.track(
    CustomEvent(
        eventName = "achievement_unlocked",
        category = "gameplay",
        value = 100.0,
        metadata = mapOf(
            "achievement_id" to "first_victory",
            "player_level" to "5"
        )
    )
)
```

### Engagement Events

Track user interactions with UI elements:

```kotlin
PulseKitAndroid.instance.track(
    EngagementEvent(
        action = EngagementAction.CLICK,
        target = "purchase_button",
        duration = 150.milliseconds,
        metadata = mapOf(
            "item_id" to "premium_pack",
            "price" to "9.99"
        )
    )
)
```

### Performance Events

Monitor app performance metrics:

```kotlin
val startTime = System.currentTimeMillis()

// ... perform operation ...

val duration = System.currentTimeMillis() - startTime

PulseKitAndroid.instance.track(
    PerformanceEvent(
        metric = "image_load_time",
        value = duration.toDouble(),
        unit = "ms",
        metadata = mapOf(
            "image_size" to "2.5MB",
            "image_format" to "JPEG"
        )
    )
)
```

### Error Events

Track errors and exceptions:

```kotlin
try {
    // Risky operation
    performRiskyOperation()
} catch (e: Exception) {
    PulseKitAndroid.instance.track(
        ErrorEvent(
            errorType = ErrorType.RUNTIME,
            message = e.message ?: "Unknown error",
            stackTrace = e.stackTraceToString(),
            isFatal = false,
            metadata = mapOf(
                "operation" to "data_sync",
                "user_id" to getCurrentUserId()
            )
        )
    )
}
```

## Configuration Options

PulseKit offers many configuration options to customize behavior:

```kotlin
val config = PulseKitConfig {
    // Authentication
    apiKey = "your-api-key"
    baseUrl = "https://api.pulsekit.dev" // Custom endpoint
    
    // Debugging
    enableDebugLogging = BuildConfig.DEBUG
    
    // Queue Management
    maxQueueSize = 1000          // Max events to queue
    flushInterval = 5.minutes    // Auto-flush interval
    maxEventAge = 24.hours       // Discard events older than this
    
    // Session Management
    enableAutoSessionManagement = true
    sessionTimeout = 30.minutes
    
    // Offline Support
    enableOfflineQueueing = true
    
    // Global Metadata (added to all events)
    metadata("app_version", BuildConfig.VERSION_NAME)
    metadata("build_type", BuildConfig.BUILD_TYPE)
    metadata("device_model", Build.MODEL)
}

PulseKitAndroid.initialize(this, config)
```

## Session Management

PulseKit automatically manages sessions based on app lifecycle, but you can also control them manually:

```kotlin
// Get current session info
val session = PulseKitAndroid.instance.getCurrentSession()
if (session != null) {
    println("Session ID: ${session.sessionId}")
    println("Session active: ${session.isActive}")
    println("Session duration: ${session.duration}")
}

// Manual session control
PulseKitAndroid.instance.startSession()
PulseKitAndroid.instance.endSession()
```

## Advanced Features

### Auto-Initialization

If you prefer automatic initialization, you can use AndroidX Startup:

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

### Error Handling

PulseKit provides type-safe error handling:

```kotlin
try {
    PulseKitAndroid.instance.track(event)
} catch (e: PulseKitError.Event.QueueFull) {
    Log.w("PulseKit", "Event queue is full")
} catch (e: PulseKitError.InitializationError) {
    Log.e("PulseKit", "SDK not initialized", e)
}
```

### Debugging

Enable debug logging to see what's happening under the hood:

```kotlin
PulseKitConfig {
    enableDebugLogging = true
}
```

This will log detailed information about event processing, session management, and network operations.

## Best Practices

### 1. Initialize Early
Initialize PulseKit in your Application's `onCreate()` to ensure it's ready before any events are tracked.

### 2. Use Meaningful Event Names
Choose descriptive event names that will be useful for analytics:

```kotlin
// Good
CustomEvent(eventName = "user_completed_onboarding")

// Avoid
CustomEvent(eventName = "event1")
```

### 3. Add Context with Metadata
Use metadata to provide context for events:

```kotlin
CustomEvent(
    eventName = "purchase_completed",
    metadata = mapOf(
        "item_category" to "premium",
        "price_usd" to "9.99",
        "payment_method" to "credit_card",
        "user_tier" to "gold"
    )
)
```

### 4. Handle Errors Gracefully
Always wrap PulseKit calls in try-catch blocks to handle potential errors:

```kotlin
try {
    PulseKitAndroid.instance.track(event)
} catch (e: PulseKitError) {
    // Handle error appropriately
    Log.e("Analytics", "Failed to track event", e)
}
```

### 5. Use Appropriate Event Types
Choose the right event type for your use case:
- `CustomEvent` for app-specific actions
- `EngagementEvent` for user interactions
- `PerformanceEvent` for metrics
- `ErrorEvent` for exceptions

## Next Steps

- Read the [Architecture Guide](Architecture.md) to understand the SDK design
- Check out the [sample app](../sample-app/) for a complete working example
- Visit the [PulseKit Dashboard](https://dashboard.pulsekit.dev) to view your analytics
- Browse the [API Documentation](api/) for detailed reference

## Need Help?

- 📖 [Documentation](../README.md)
- 🐛 [Report Issues](https://github.com/pulsekit/pulsekit/issues)
- 💬 [Community Forum](https://github.com/pulsekit/pulsekit/discussions)
- 📧 [Email Support](mailto:support@pulsekit.dev)
