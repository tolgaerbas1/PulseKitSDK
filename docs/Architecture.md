# PulseKit Architecture Guide

This document explains the architecture and design principles behind PulseKit.

## Overview

PulseKit is built with a clean, modular architecture that separates concerns and provides a production-grade foundation for telemetry in Android apps and games.

## Design Principles

### 1. Developer-First API
- **Type Safety**: Sealed classes prevent invalid event types
- **Impossible to Misuse**: API design prevents common errors
- **Clean DSL**: Configuration uses Kotlin DSL for readability
- **Single Entry Point**: `PulseKit.initialize()` is the only way to start

### 2. Offline-First Architecture
- **Local Queueing**: Events are stored locally when offline
- **Automatic Sync**: Events are sent when connectivity is restored
- **Burst Handling**: Can handle high-frequency events without data loss
- **Graceful Degradation**: Works even with poor network conditions

### 3. Lifecycle-Aware Design
- **Automatic Sessions**: Sessions start/stop with app lifecycle
- **Resource Management**: Proper cleanup when app is backgrounded
- **Battery Efficient**: Minimal impact on battery life
- **Memory Conscious**: Bounded queues prevent memory leaks

### 4. Multi-Platform Foundation
- **Kotlin Multiplatform**: Core logic is platform-agnostic
- **Android Integration**: Android-specific features in separate module
- **Future-Ready**: Designed for Unity/Unreal integration
- **Clean Separation**: No Android dependencies in core module

## Module Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Root Project                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │pulsekit-core│  │pulsekit-    │  │    build-logic      │  │
│  │             │  │android      │  │                     │  │
│  │• Public API │  │• Android    │  │• Convention Plugins│  │
│  │• Events     │  │  Bindings   │  │• Publishing Setup   │  │
│  │• Sessions   │  │• Lifecycle  │  │• Version Management │  │
│  │• Queue      │  │• Network    │  │                     │  │
│  │• Storage    │  │• Auto-Init  │  │                     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│                           │                                 │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                  sample-app                         │    │
│  │                                                     │    │
│  │• Demo Usage                                         │    │
│  │• Integration Examples                               │    │
│  │• Testing Ground                                     │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### pulsekit-core

The heart of the SDK, containing all business logic without any platform dependencies.

**Key Components:**

- **`PulseKit`**: Main entry point object
- **`PulseKitInstance`**: The actual SDK instance
- **`PulseKitConfig`**: Configuration with DSL builder
- **Event Types**: Sealed class hierarchy for all events
- **Error Types**: Comprehensive error handling
- **SessionManager**: Lifecycle-aware session management
- **EventQueue**: Offline-first event storage
- **EventProcessor**: Event validation and enrichment

**Architecture Benefits:**
- Platform agnostic - can be used in any Kotlin environment
- Easy to test - no Android dependencies
- Clean separation of concerns
- Future-proof for other platforms

### pulsekit-android

Android-specific bindings and integrations.

**Key Components:**

- **`PulseKitAndroid`**: Android-specific entry point
- **`PulseKitLifecycleObserver`**: ProcessLifecycleOwner integration
- **`NetworkMonitor`**: Real-time connectivity monitoring
- **`PulseKitInitializer`**: AndroidX Startup integration

**Architecture Benefits:**
- Clean Android integration
- Automatic lifecycle management
- Network-aware operation
- Minimal Android dependencies

### build-logic

Gradle convention plugins for consistent builds across modules.

**Key Components:**

- **KotlinMultiplatformLibraryPlugin**: Core module build logic
- **AndroidLibraryPlugin**: Android module build logic
- **AndroidApplicationPlugin**: Sample app build logic

**Architecture Benefits:**
- Consistent build configuration
- DRY principle - no repeated build logic
- Easy to maintain and update
- Publishing-ready setup

## Core Components Deep Dive

### Event System

The event system is built around sealed classes for type safety:

```kotlin
sealed class PulseEvent {
    abstract val eventName: String
    abstract val metadata: Map<String, String>
}

class CustomEvent(
    override val eventName: String,
    override val metadata: Map<String, String>,
    val value: Double? = null,
    val category: String? = null
) : PulseEvent()
```

**Benefits:**
- Compile-time safety - no invalid event types
- Exhaustive when statements
- Easy to extend with new event types
- Clear inheritance hierarchy

### Session Management

Sessions are automatically managed based on app lifecycle:

```kotlin
class SessionManager {
    fun startNewSession()
    fun endCurrentSession()
    fun refreshSession()
    fun getCurrentSession(): SessionInfo?
}
```

**Key Features:**
- Automatic start/stop with app lifecycle
- Configurable timeout handling
- Session persistence across app restarts
- Rich session metadata

### Event Queue

The queue is designed for offline-first operation:

```kotlin
class EventQueue {
    fun enqueue(event: PulseEvent)
    fun getNextBatch(batchSize: Int): List<PulseEvent>
    fun markProcessed(events: List<PulseEvent>)
    fun markFailed(event: PulseEvent)
    fun flush()
}
```

**Key Features:**
- Bounded size prevents memory issues
- Automatic expiration of old events
- Batch processing for efficiency
- Retry logic for failed events

### Configuration System

Configuration uses a clean DSL pattern:

```kotlin
val config = PulseKitConfig {
    apiKey = "your-api-key"
    enableDebugLogging = true
    sessionTimeout = 30.minutes
    metadata("app_version", "1.0.0")
}
```

**Benefits:**
- Type-safe configuration
- Default values for simplicity
- IDE auto-completion
- Clear parameter names

## Data Flow

```
User Action
    │
    ▼
Event Creation
    │
    ▼
Event Validation
    │
    ▼
Event Enrichment
    │
    ▼
Queue Event
    │
    ▼
Batch Processing
    │
    ▼
Network Send
    │
    ▼
Success/Failure Handling
```

### Event Processing Pipeline

1. **Event Creation**: User creates an event instance
2. **Validation**: Event is validated for size, required fields, etc.
3. **Enrichment**: Global metadata, session info, device info added
4. **Queuing**: Event is added to local queue
5. **Batching**: Events are grouped for efficient sending
6. **Network**: Batch is sent to server
7. **Handling**: Success removes events, failure retries or drops

## Error Handling Strategy

PulseKit uses a comprehensive error hierarchy:

```kotlin
sealed interface PulseKitError {
    sealed class InitializationError : PulseKitError
    sealed class EventError : PulseKitError
    sealed class NetworkError : PulseKitError
    sealed class StorageError : PulseKitError
    sealed class SessionError : PulseKitError
}
```

**Principles:**
- Type-safe error handling
- Never crash the host app
- Graceful degradation
- Detailed error context

## Performance Considerations

### Memory Management
- Bounded event queue (configurable size)
- Automatic cleanup of expired events
- Efficient data structures
- Minimal object allocation

### Battery Efficiency
- Batch network requests
- Background processing limits
- Lifecycle-aware operation
- Minimal wake locks

### Network Efficiency
- Compressed event payloads
- Intelligent batching
- Retry with exponential backoff
- Network-aware scheduling

## Testing Strategy

### Unit Testing
- Pure Kotlin functions in core module
- Mocked dependencies
- Comprehensive edge case coverage
- Property-based testing where applicable

### Integration Testing
- Android component interactions
- Network behavior simulation
- Lifecycle event handling
- End-to-end event flow

### Performance Testing
- Memory usage under load
- Battery impact measurement
- Network efficiency validation
- Startup time measurement

## Future Architecture Plans

### Multi-Platform Expansion
- iOS module (Swift/Kotlin Native)
- Web module (JavaScript/TypeScript)
- Unity plugin
- Unreal Engine plugin

### Advanced Features
- Real-time event streaming
- Machine learning integration
- Advanced analytics processing
- Custom event processors

### Performance Optimizations
- Database persistence option
- Compression algorithms
- Adaptive batching
- Predictive pre-fetching

## Contributing to Architecture

When contributing to PulseKit's architecture:

1. **Maintain Separation**: Keep platform-specific code in appropriate modules
2. **Type Safety**: Use sealed classes and value classes
3. **Error Handling**: Never let SDK errors crash the host app
4. **Documentation**: Update architecture docs for significant changes
5. **Testing**: Ensure comprehensive test coverage
6. **Performance**: Consider impact on memory, battery, and network
7. **API Compatibility**: Maintain backward compatibility, see [API Compatibility Guide](ApiCompatibility.md)

## API Compatibility Architecture

PulseKit includes a comprehensive API compatibility system to ensure backward compatibility:

### Compatibility Layers

1. **Source Compatibility**: API signature comparison using snapshots
2. **Binary Compatibility**: JVM bytecode compatibility with japicmp
3. **Runtime Compatibility**: Behavioral compatibility validation

### Compatibility Tools

- **API Snapshots**: Baseline API signatures stored in `api/baseline/`
- **japicmp**: Binary compatibility checking against published artifacts
- **Automated Validation**: CI/CD integration with fail-fast behavior
- **Developer Workflows**: Clear processes for intentional breaking changes

### Compatibility Process

```bash
# Daily development
./gradlew checkApiCompatibility checkBinaryCompatibility

# Intentional breaking changes
./gradlew updateApiBaselines
./gradlew versionMajor

# Force compatibility (emergency)
./gradlew checkApiCompatibility -PFORCE_API_COMPATIBILITY=true
```

### Compatibility Rules

- **Non-Breaking**: Adding new classes/methods, expanding return types
- **Breaking**: Removing public APIs, changing signatures, changing visibility
- **Validation**: Automatic detection in CI/CD with detailed reporting

For detailed information, see the [API Compatibility Guide](ApiCompatibility.md).

## Feature Flag Architecture

PulseKit includes a simplified server-driven feature flag system for remote behavior control:

### Flag Categories

- **Performance Flags**: Batch size, compression, retry logic
- **Behavioral Flags**: Session management, offline queueing, persistence
- **Debugging Flags**: Logging, monitoring, diagnostics

### Simplified Implementation

```kotlin
// Core FeatureFlagManager with server overrides, in-memory caching, and optional disk persistence
// Initialized via PulseKitInstance.configureFeatureFlags(networkClient, persistence)
val instance = PulseKit.initialize(config, scope, batchSender)
instance.configureFeatureFlags(androidNetworkClient, flagPersistence)

// Read flags with type-safe access and automatic server fallback
val batchSize = instance.getIntegerFlag(PulseKitFeatureFlags.EVENT_BATCH_SIZE)
val compression = instance.getBooleanFlag(PulseKitFeatureFlags.EVENT_COMPRESSION)
```

### Use Cases

- **Remote Configuration**: Change behavior without app updates
- **Performance Tuning**: Adjust batch sizes and retry logic
- **Emergency Controls**: Disable problematic features immediately

For detailed information, see the [Feature Flags Guide](FeatureFlags.md).

## Backpressure Strategy Architecture

PulseKit includes a comprehensive backpressure system to manage event queue overflow and ensure app stability:

### Queue Management

- **Memory Queue**: 1,000 events (configurable)
- **Disk Queue**: 10,000 events (configurable)
- **Backpressure Threshold**: 90% capacity trigger
- **Fail-Safe**: Never crash host app

### Simplified Priority System

- **CRITICAL**: Error events, crashes (never dropped)
- **HIGH**: Lifecycle, session, performance events
- **LOW**: User interactions, debug events

### Implementation

```kotlin
// Simplified priority calculation
val priority = SimplifiedPriorityCalculator.calculatePriority(event)
val droppedCount = backpressureManager.applyBackpressure(events, capacity, "memory")
```

### Use Cases

- **Memory Protection**: Prevents unbounded growth
- **App Stability**: Never crashes host app
- **Data Prioritization**: Preserves critical events
- **Predictable Behavior**: Deterministic dropping logic

For detailed information, see the [Backpressure Strategy Guide](BackpressureStrategy.md).

## Simplified Metrics Architecture

PulseKit includes lightweight metrics for internal debugging:

### Essential Metrics Only

- **Dropped Event Count**: Total events dropped by backpressure
- **Last Drop Reason**: Reason for most recent drop
- **Last Drop Timestamp**: When last drop occurred
- **Queue Utilization**: Current queue capacity usage

### Implementation

```kotlin
// Simple metrics collection
metrics.recordMemoryDrop(count, "queue_overflow")
val metricsData = metrics.getMetrics()
```

### Use Cases

- **Debugging**: Understand why events are being dropped
- **Monitoring**: Track queue health
- **Optimization**: Identify performance bottlenecks

For detailed information, see the [Design Tradeoffs](DesignTradeoffs.md).

## Conclusion

PulseKit's architecture is designed for production use with a focus on:
- **Reliability**: Works offline, handles errors gracefully
- **Performance**: Minimal impact on app performance
- **Maintainability**: Clean, modular, well-documented code
- **Extensibility**: Easy to add new features and platforms
- **Developer Experience**: Clean API that's impossible to misuse
- **Backward Compatibility**: Comprehensive API compatibility system
- **Remote Control**: Simplified server-driven feature flags for behavior control
- **Stability**: Comprehensive backpressure strategy for queue management
- **Simplicity**: Intentionally simplified systems where complexity isn't justified

This architecture ensures PulseKit can scale from small apps to large games while maintaining the same level of reliability and performance, preserves backward compatibility for existing users, provides remote control capabilities for dynamic behavior management, and guarantees app stability under all conditions through its robust backpressure system. The simplified design reduces maintenance burden while preserving all essential safety guarantees.

For detailed information on engineering decisions and tradeoffs, see the [Design Tradeoffs](DesignTradeoffs.md).
