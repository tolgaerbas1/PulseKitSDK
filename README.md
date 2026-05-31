# PulseKit

[![Maven Central](https://img.shields.io/maven-central/v/com.pulsekit/pulsekit-android.svg)](https://search.maven.org/artifact/com.pulsekit/pulsekit-android)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android-arsenal.com/api?level=21)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)

Production-grade telemetry SDK for Android apps and games, built with a developer-first API design, lifecycle-aware, and offline-first architecture.

## Features

- 🎯 **Developer-First API** - Clean, intuitive API that's impossible to misuse
- 🔄 **Lifecycle-Aware** - Automatic session management with ProcessLifecycleOwner
- 📱 **Offline-First** - Queue events locally when offline, sync when connected
- 🏗️ **Multi-Module Architecture** - Clean separation between core and Android-specific code
- 🧵 **Coroutine-Based** - Built on structured concurrency for modern async programming
- 🔒 **Type-Safe** - Sealed classes for events and errors prevent runtime surprises
- 📦 **Ready for Publishing** - Maven Central compatible with proper documentation
- 🎮 **Game Engine Ready** - Designed for future Unity/Unreal integration

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
┌─────────────────┐    ┌─────────────────┐
│   pulsekit-core │    │ pulsekit-android│
│                 │    │                 │
│ • Public API    │◄──►│ • Android Bindings
│ • Event Types   │    │ • Lifecycle     │
│ • Session Mgmt  │    │ • Network       │
│ • Queue Logic   │    │ • Auto-Init     │
└─────────────────┘    └─────────────────┘
         │                       │
         ▼                       ▼
┌─────────────────┐    ┌─────────────────┐
│   build-logic   │    │   sample-app    │
│                 │    │                 │
│ • Convention    │    │ • Demo Usage    │
│   Plugins       │    │ • Testing       │
│ • Publishing    │    │ • Examples      │
└─────────────────┘    └─────────────────┘
```

### Module Overview

- **pulsekit-core**: Pure Kotlin module with public API, no Android dependencies
- **pulsekit-android**: Android-specific bindings, lifecycle integration, network monitoring
- **sample-app**: Minimal Android app demonstrating SDK usage
- **build-logic**: Gradle convention plugins for consistent builds
- **docs**: Documentation and guides

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

PulseKit uses a clean DSL-based configuration:

```kotlin
val config = PulseKitConfig {
    // Required for production - use BuildConfig or env; never hardcode
    apiKey = BuildConfig.PULSEKIT_API_KEY
    
    // Optional settings
    baseUrl = "https://api.pulsekit.dev"
    enableDebugLogging = BuildConfig.DEBUG
    maxQueueSize = 1000
    flushInterval = 5.minutes
    sessionTimeout = 30.minutes
    
    // Global metadata added to all events
    metadata("app_version", "1.0.0")
    metadata("build_type", "release")
}
```

**API Key and Production:** See [API Key and Backend Guide](docs/ApiKeyAndBackend.md) for secure setup with BuildConfig, local.properties, or environment variables.

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

## Production note

**API key:** In production, provide the API key via BuildConfig, environment variables, or a secure secrets mechanism. Do not commit API keys in source code.

## Production note

**API key:** In production, provide the API key via BuildConfig, environment variables, or a secure secrets mechanism. Do not commit API keys in source code.

## Requirements

- **Android API**: 21+ (Android 5.0)
- **Kotlin**: 2.0.21+
- **Gradle**: 8.0+
- **Coroutines**: 1.7.3+

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
