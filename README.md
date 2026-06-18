# PulseKit

[![Maven Central](https://img.shields.io/maven-central/v/com.pulsekit/pulsekit-android.svg)](https://search.maven.org/artifact/com.pulsekit/pulsekit-android)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android-arsenal.com/api?level=21)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![AGP](https://img.shields.io/badge/AGP-8.7.3-green.svg)](https://developer.android.com/build/releases/gradle-plugin)
[![Gradle](https://img.shields.io/badge/Gradle-8.10-02303a.svg)](https://gradle.org)

Production-grade telemetry SDK for Android apps and games. Developer-first API design, lifecycle-aware, offline-first architecture with disk-backed event persistence and server-driven feature flags.

## Features

- 🎯 **Developer-First API** - Clean, intuitive API that's impossible to misuse
- 🔄 **Lifecycle-Aware** - Automatic session management via ProcessLifecycleOwner
- 📱 **Offline-First** - Queue events locally when offline, sync when connected
- 💾 **Disk-Backed Persistence** - Events survive app restarts via SQLite storage
- 🚩 **Server-Driven Feature Flags** - Remote behavior control without app updates
- 🏗️ **Multi-Module Architecture** - Clean separation between core and Android-specific code
- 🧵 **Coroutine-Based** - Structured concurrency for modern async programming
- 🔒 **Type-Safe** - Sealed classes for events and errors prevent runtime surprises
- 🛡️ **Host App Safety** - Bounded queues, deterministic drop policies, never crashes the host
- ⚡ **Backpressure Management** - Priority-based event dropping under extreme load
- 📊 **Crash Reporting** - Opt-in uncaught exception tracking as fatal ErrorEvents
- 🔌 **Network Monitoring** - Automatic event flush when connectivity is restored
- 📦 **Maven Central Ready** - Publishing automation with API compatibility checking

## Quick Start

### 1. Add Dependency

```gradle
dependencies {
    implementation("com.pulsekit:pulsekit-android:0.1.0")
}
```

### 2. Initialize in Application

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        PulseKitAndroid.initialize(this, PulseKitConfig {
            apiKey = BuildConfig.PULSEKIT_API_KEY  // See docs/ApiKeyAndBackend.md for production setup
            enableDebugLogging = BuildConfig.DEBUG
            enableAutoSessionManagement = true
            enableOfflineQueueing = true
        })
    }
}
```

### 3. Track Events

```kotlin
// Track custom events
PulseKitAndroid.instance.track(
    CustomEvent(
        eventName = "button_clicked",
        category = "ui_interaction",
        metadata = mapOf(
            "button_id" to "signup_button",
            "screen" to "onboarding"
        )
    )
)

// Track engagement events
PulseKitAndroid.instance.track(
    EngagementEvent(
        action = EngagementAction.CLICK,
        target = "signup_button",
        duration = 150.milliseconds
    )
)

// Track performance events
PulseKitAndroid.instance.track(
    PerformanceEvent(
        metric = "app_startup_time",
        value = startupTimeMs.toDouble(),
        unit = "ms"
    )
)
```

## Architecture

PulseKit is built with a clean multi-module architecture:

```
┌──────────────────────────────────────────────┐
│              pulsekit-core (KMP)             │
│                                              │
│  • Public API (PulseKit, PulseKitInstance)   │
│  • Event Types (sealed class hierarchy)      │
│  • Session Management (SessionManager)       │
│  • Event Queue (in-memory + disk-backed)     │
│  • Backpressure (priority-based dropping)    │
│  • Feature Flag Manager (TTL cache)          │
│  • Flag Persistence (disk storage)           │
│  • Database Driver (platform-agnostic)       │
└────────────────┬─────────────────────────────┘
                 │ depends on
┌────────────────▼─────────────────────────────┐
│            pulsekit-android                  │
│                                              │
│  • PulseKitAndroid (zero-config entry)       │
│  • Lifecycle Observer (ProcessLifecycleOwner)│
│  • Network Monitor (ConnectivityManager)     │
│  • Crash Reporting (UncaughtExceptionHandler)│
│  • Android SQLite Driver (database)          │
│  • Android Network Client (HttpURLConnection)│
│  • Android Flag Storage (SharedPreferences)  │
└────────────────┬─────────────────────────────┘
                 │
┌────────────────▼─────────────────────────────┐
│               sample-app                     │
│  • Demo usage, testing, examples             │
└──────────────────────────────────────────────┘
```

### Module Overview

- **pulsekit-core**: Pure Kotlin KMP module with public API, no Android dependencies
- **pulsekit-android**: Android bindings — lifecycle, network, database, feature flags
- **sample-app**: Minimal Android app demonstrating SDK usage
- **build-logic**: Gradle convention plugins for consistent builds
- **docs**: Architecture guides, feature documentation, staff reviews, and the [Medium article series plan](docs/MediumArticleSeriesPlan.md)

### Key Design Decisions

| Decision | Rationale | Documented in |
|---|---|---|
| Feature Flag Manager + persistence | Offline resilience, battery efficiency via lifecycle-aware fetch | `docs/FeatureFlags.md` |
| In-memory queue + SQLite backing | Fast reads + survive restarts | `docs/Architecture.md` |
| Backpressure with priority dropping | Host app never crashes, critical events preserved | `docs/BackpressureStrategy.md` |
| `DatabaseDriver` interface | Platform-agnostic; Android uses SQLite, JVM uses JDBC | `docs/Architecture.md` |

## Event Types

PulseKit provides type-safe event classes for all common tracking scenarios:

### Custom Events
```kotlin
CustomEvent(
    eventName = "achievement_unlocked",
    category = "gameplay",
    value = 100.0,
    metadata = mapOf("achievement_id" to "first_blood")
)
```

### Engagement Events
```kotlin
EngagementEvent(
    action = EngagementAction.CLICK,
    target = "play_button",
    duration = 150.milliseconds
)
```

### Performance Events
```kotlin
PerformanceEvent(
    metric = "frame_time",
    value = 16.7,
    unit = "ms"
)
```

### Error Events
```kotlin
ErrorEvent(
    errorType = ErrorType.RUNTIME,
    message = "Null pointer exception",
    stackTrace = exception.stackTraceToString(),
    isFatal = false
)
```

### Lifecycle Events
```kotlin
LifecycleEvent(
    action = LifecycleAction.FOREGROUND,
    component = "MainActivity"
)
```

## Configuration

PulseKit uses a clean DSL-based configuration. All optional settings have sensible production defaults.

```kotlin
val config = PulseKitConfig {
    apiKey = BuildConfig.PULSEKIT_API_KEY
    
    // Offline queue & persistence (defaults shown)
    enableOfflineQueueing = true      // Queue events when offline
    enableDiskPersistence = true      // Survive app restarts via SQLite
    
    // Session management
    enableAutoSessionManagement = true
    sessionTimeout = 30.minutes
    
    // Performance tuning
    maxQueueSize = 1000               // Max in-memory events
    flushInterval = 5.minutes         // Auto-flush interval
    maxEventAge = 24.hours            // Discard stale events
    
    // Debug & crash reporting
    enableDebugLogging = BuildConfig.DEBUG
    enableCrashReporting = false      // Opt-in uncaught exception tracking
    
    // Global metadata added to all events
    metadata("app_version", BuildConfig.VERSION_NAME)
    metadata("build_type", BuildConfig.BUILD_TYPE)
}
```

**Feature flags** are automatically enabled. The SDK fetches server-driven configuration every 5 minutes and persists flags to disk for offline availability. See the [Feature Flags Guide](docs/FeatureFlags.md).

**API Key and Production:** See [API Key and Backend Guide](docs/ApiKeyAndBackend.md) for secure setup with BuildConfig, local.properties, or environment variables. Never commit API keys in source code.

## Session Management

PulseKit automatically manages sessions based on app lifecycle:

```kotlin
// Get current session info
val session = PulseKitAndroid.instance.getCurrentSession()
println("Session active: ${session?.isActive}")
println("Session duration: ${session?.duration}")

// Manual session control (if needed)
PulseKitAndroid.instance.startSession()
PulseKitAndroid.instance.endSession()
```

## Error Handling

All PulseKit errors are type-safe through sealed interfaces:

```kotlin
try {
    PulseKitAndroid.instance.track(event)
} catch (e: PulseKitError.Event.QueueFull) {
    // Handle queue full scenario
    Log.w("PulseKit", "Event queue is full, dropping events")
} catch (e: PulseKitError.InitializationError) {
    // Handle initialization issues
    Log.e("PulseKit", "SDK not properly initialized", e)
}
```

## Auto-Initialization

For convenience, PulseKit supports AndroidX Startup auto-initialization:

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
        android:name="com.pulsekit.enable_debug"
        android:value="true" />
</provider>
```

## Building

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew test

# Build sample app
./gradlew :sample-app:assembleDebug

# Generate documentation
./gradlew dokkaHtml
```

## Publishing

This project is configured for Maven Central publishing:

```bash
# Publish to staging
./gradlew publishToMavenLocal

# Publish to Maven Central (requires proper setup)
./gradlew publishAllPublicationsToMavenCentralRepository
```

## License

```
Copyright 2024 PulseKit Team

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

## Support

- 📖 [Documentation](docs/)
- 🐛 [Issue Tracker](https://github.com/pulsekit/pulsekit/issues)
- 💬 [Discussions](https://github.com/pulsekit/pulsekit/discussions)
- 📧 [Email](mailto:support@pulsekit.dev)
