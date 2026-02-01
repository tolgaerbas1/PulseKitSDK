# PulseKit Core

The core module of PulseKit SDK, providing platform-agnostic telemetry functionality.

## Overview

PulseKit Core is a pure Kotlin module that contains all the essential telemetry features without any platform dependencies. This makes it suitable for use in any Kotlin environment, including JVM, Android, iOS (via Kotlin Native), and JavaScript (via Kotlin/JS).

## Key Features

- **Type-Safe Event Tracking**: Sealed classes ensure compile-time safety
- **Session Management**: Automatic session lifecycle management
- **Offline-First Queueing**: Events are queued locally when offline
- **Error Handling**: Comprehensive error hierarchy for graceful degradation
- **Configuration**: Flexible DSL-based configuration system
- **Serialization**: Built-in JSON serialization for event persistence

## Architecture

The core module follows a clean architecture pattern with clear separation of concerns:

### Public API (`com.pulsekit.core.api`)
- `PulseKit`: Main entry point for SDK initialization
- `PulseKitInstance`: The actual SDK instance providing all functionality
- `PulseKitConfig`: Configuration with DSL builder pattern
- Event types: `CustomEvent`, `EngagementEvent`, `PerformanceEvent`, etc.
- Error types: Comprehensive sealed interface hierarchy

### Internal Implementation (`com.pulsekit.core.api.internal`)
- `EventQueue`: Offline-first event storage and processing
- `SessionManager`: Lifecycle-aware session management
- `EventProcessor`: Event validation and enrichment
- `Storage`: Database abstraction for persistence

## Usage

### Basic Setup

```kotlin
// Initialize with default configuration
val pulseKit = PulseKit.initialize()

// Or with custom configuration
val config = PulseKitConfig {
    apiKey = "your-api-key"
    enableDebugLogging = true
    maxQueueSize = 1000
    flushInterval = 5.minutes
}

val pulseKit = PulseKit.initialize(config)
```

### Event Tracking

```kotlin
// Track custom events
pulseKit.track(
    CustomEvent(
        eventName = "user_action",
        category = "engagement",
        value = 42.0,
        metadata = mapOf(
            "action_type" to "button_click",
            "screen" to "main"
        )
    )
)

// Track engagement events
pulseKit.track(
    EngagementEvent(
        action = EngagementAction.CLICK,
        target = "signup_button",
        duration = 150.milliseconds
    )
)

// Track performance events
pulseKit.track(
    PerformanceEvent(
        metric = "app_startup_time",
        value = startupTimeMs.toDouble(),
        unit = "ms"
    )
)
```

### Session Management

```kotlin
// Get current session info
val session = pulseKit.getCurrentSession()
println("Session active: ${session?.isActive}")
println("Session duration: ${session?.duration}")

// Manual session control (optional)
pulseKit.startSession()
pulseKit.endSession()
```

## Configuration Options

```kotlin
val config = PulseKitConfig {
    // Authentication
    apiKey = "your-api-key"
    baseUrl = "https://api.pulsekit.dev"
    
    // Debugging
    enableDebugLogging = false
    
    // Queue Management
    maxQueueSize = 1000
    flushInterval = 5.minutes
    maxEventAge = 24.hours
    
    // Session Management
    enableAutoSessionManagement = true
    sessionTimeout = 30.minutes
    
    // Offline Support
    enableOfflineQueueing = true
    enableDiskPersistence = true
    maxDatabaseSize = 50 * 1024 * 1024 // 50MB
    
    // Global Metadata
    metadata("app_version", "1.0.0")
    metadata("build_type", "release")
}
```

## Error Handling

PulseKit provides comprehensive error handling through sealed interfaces:

```kotlin
try {
    pulseKit.track(event)
} catch (e: PulseKitError.Event.QueueFull) {
    // Handle queue full scenario
    Log.w("PulseKit", "Event queue is full")
} catch (e: PulseKitError.InitializationError) {
    // Handle initialization issues
    Log.e("PulseKit", "SDK not properly initialized", e)
}
```

## Platform Integration

While this module is platform-agnostic, it's designed to work seamlessly with platform-specific modules:

- **Android**: Use `pulsekit-android` for lifecycle integration and Android-specific features
- **iOS**: Future support via Kotlin Native
- **JavaScript**: Future support via Kotlin/JS

## Thread Safety

All public APIs are thread-safe and can be called from any thread. Internally, the SDK uses structured concurrency with coroutines for optimal performance.

## Memory Management

- Bounded event queues prevent memory leaks
- Automatic cleanup of expired events
- Efficient serialization and storage
- Minimal object allocation during event processing

## Testing

The module includes comprehensive unit tests covering:
- Event serialization/deserialization
- Session management
- Queue operations
- Error handling
- Configuration validation

Run tests with:
```bash
./gradlew :pulsekit-core:test
```

## Dependencies

- Kotlin Coroutines: For structured concurrency
- Kotlinx Serialization: For JSON serialization
- SQLite (platform-specific): For disk persistence

## License

Apache License 2.0 - see LICENSE file for details.
