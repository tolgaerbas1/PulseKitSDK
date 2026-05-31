# Feature Flags Guide

This guide explains PulseKit's server-driven feature flag system, which enables remote behavior control without changing the public SDK API.

## Overview

PulseKit includes a comprehensive feature flag system that allows:

- **Gradual Rollouts**: Enable features for specific user segments
- **Safe Experimentation**: Test new behaviors with kill-switch capability
- **Remote Configuration**: Change SDK behavior without app updates
- **Performance Optimization**: Adjust batch sizes, compression, and retry logic
- **Emergency Controls**: Disable problematic features immediately

## Architecture

### Design Decision

**Chosen**: Single unified `FeatureFlagManager` with persistence and server-backed periodic refresh.

**Trade-off**: This design retains enterprise-grade caching (5-min TTL), disk persistence for offline availability, and lifecycle-aware network fetching — all necessary for a professional production SDK. The complexity is justified by the need for:
- **Offline resilience**: Flags are usable immediately after app launch via persistence layer
- **Battery efficiency**: Lifecycle-aware fetching skips network when app is in background
- **Type safety**: Sealed `FlagValue` hierarchy prevents misuse
- **Observability**: Fetch status tracking for debugging

### Flag Types

```kotlin
sealed class FlagValue {
    data class BooleanValue(val value: Boolean)
    data class IntegerValue(val value: Long)
    data class DoubleValue(val value: Double)
    data class StringValue(val value: String)
}
```

### Flag Categories

#### **Performance Flags**
- `event_batch_size`: Maximum events per batch (default: 50)
- `event_compression`: Enable payload compression (default: true)
- `max_retry_attempts`: Retry attempts for failed events (default: 3)
- `exponential_backoff`: Enable exponential backoff (default: true)

#### **Behavioral Flags**
- `session_timeout_minutes`: Session timeout duration (default: 30)
- `offline_queueing`: Enable offline queueing (default: true)
- `max_queue_size`: Maximum queue size (default: 1000)
- `disk_persistence`: Enable disk persistence (default: true)
- `flush_interval_minutes`: Auto-flush interval (default: 5)

#### **Debugging Flags**
- `debug_logging`: Enable debug logging (default: false)
- `network_monitoring`: Enable network monitoring (default: true)

#### **Experimental Flags**
- `experimental_retry_logic`: Experimental retry logic (default: false)
- `event_deduplication`: Event deduplication (default: false)

## Implementation Details

### Component Architecture

| Component | Location | Role |
|---|---|---|
| `FeatureFlagManager` | `pulsekit-core` | In-memory cache with 5-min TTL, type-safe getters, `refreshAction` hook |
| `FeatureFlagService` | `pulsekit-core` | Server communication, JSON parsing, periodic fetching |
| `FlagPersistence` | `pulsekit-core` | Disk persistence via `FlagStorage` & `PlatformFlagStorage` |
| `AndroidNetworkClient` | `pulsekit-android` | `HttpURLConnection`-based `NetworkClient` with lifecycle awareness |
| `AndroidFileFlagStorage` | `pulsekit-android` | Android internal storage for persisted flags |
| `PulseKitInstance` | `pulsekit-core` | Wiring point: `configureFeatureFlags(networkClient, persistence)` |

### Flag Evaluation Flow

1. **Default Values**: `FeatureFlagManager.init` seeds all flags with hardcoded defaults
2. **Persistence Load**: `configureFeatureFlags()` loads persisted values from disk → updates manager
3. **In-Memory Cache**: All `getBooleanFlag()` etc. read from cache (5-min TTL)
4. **Server Refresh**: On TTL expiry, `refreshAction` triggers `FeatureFlagService.fetchFeatureFlags()` → updates cache
5. **Persistence Save**: Successful server responses are persisted to disk for offline availability
6. **Fallback**: Server unavailable → continue using cached/persisted values; never crashes

### Thread Safety

- All flag evaluations read from `MutableMap` with coroutine-safe access
- Server updates happen in background coroutines within SDK scope
- `refreshAction` enforces single-fetch-at-a-time via `FetchStatus` state machine

### Caching Strategy

```
┌─────────────────┐    ┌─────────────────┐    ┌──────────────────┐
│   In-Memory     │───▶│   Disk Cache    │◀───│   Periodic Fetch  │
│   (5-min TTL)   │    │   (FlagPersist) │    │   (5-min interval)│
└─────────────────┘    └─────────────────┘    └──────────────────┘
                              │                          │
                              ▼                          ▼
                  AndroidFileFlagStorage        AndroidNetworkClient
                  (internal storage)           (HttpURLConnection)
```

## Usage Examples

### Performance Optimization

```kotlin
// Server can adjust batch size based on network conditions
// Flag: event_batch_size = 100 (increase for fast networks)
// Flag: event_compression = true (enable for slow networks)
```

### Gradual Rollout

```kotlin
// Enable experimental features for 10% of users
// Flag: experimental_retry_logic = true (for specific user segment)
// Flag: event_deduplication = true (for beta testers)
```

### Emergency Controls

```kotlin
// Disable problematic feature immediately
// Flag: offline_queueing = false (if queue is causing issues)
// Flag: debug_logging = true (for debugging)
```

## Server Integration

### API Endpoint

```
GET /api/v1/feature-flags
```

### Response Format

```json
{
  "flags": {
    "event_batch_size": 100,
    "event_compression": true,
    "max_retry_attempts": 5,
    "experimental_retry_logic": true
  },
  "timestamp": 1640995200000,
  "version": "1.0.0"
}
```

### Client Implementation

```kotlin
// Automatic flag fetching
val flagService = FeatureFlagService(networkClient, flagManager, scope)
flagService.startPeriodicFetching(intervalMs = 300000) // 5 minutes
```

## Rollout Strategy

### 1. Internal Testing
```json
{
  "experimental_retry_logic": true,
  "event_deduplication": true
}
```

### 2. Beta Rollout
```json
{
  "experimental_retry_logic": true,
  "event_deduplication": true,
  "max_retry_attempts": 5
}
```

### 3. Gradual Release
```json
{
  "experimental_retry_logic": true,
  "event_deduplication": true,
  "max_retry_attempts": 5,
  "event_batch_size": 75
}
```

### 4. Full Release
```json
{
  "experimental_retry_logic": true,
  "event_deduplication": true,
  "max_retry_attempts": 5,
  "event_batch_size": 100,
  "event_compression": true
}
```

## Kill Switch Scenarios

### Network Issues
```json
{
  "offline_queueing": false,
  "max_queue_size": 100,
  "flush_interval_minutes": 1
}
```

### Performance Problems
```json
{
  "event_compression": true,
  "event_batch_size": 25,
  "debug_logging": false
}
```

### Emergency Disable
```json
{
  "offline_queueing": false,
  "disk_persistence": false,
  "network_monitoring": false
}
```

## Monitoring and Observability

### Flag Fetch Metrics

```kotlin
// Track flag fetch success/failure
trackFlagFetch(success: Boolean, flagCount: Int, error: Throwable?)
```

### Active Flag Tracking

```kotlin
// Track active experimental flags
val activeFlags = flagManager.getActiveExperimentalFlags()
trackActiveFlags(activeFlags)
```

### Performance Impact

```kotlin
// Monitor flag evaluation performance
val evaluationTime = measureTime {
    flagManager.getBooleanFlag(PulseKitFeatureFlags.EVENT_COMPRESSION)
}
trackFlagEvaluation("event_compression", evaluationTime)
```

## Debugging

### Current Flag Values

```kotlin
// Get all current flag values
val flagValues = PulseKitAndroid.getFeatureFlagValues()
println("Current flags: $flagValues")
```

### Flag Status

```kotlin
// Check specific flag status
val compressionEnabled = PulseKitAndroid.isFeatureFlagEnabled(
    PulseKitFeatureFlags.EVENT_COMPRESSION
)
println("Compression enabled: $compressionEnabled")
```

### SDK Status

```kotlin
// Get SDK status with flag information
val status = PulseKitAndroid.instance.getStatus()
println("Active experimental flags: ${status.activeFeatureFlags}")
```

## Best Practices

### Flag Design

1. **Clear Naming**: Use descriptive flag names
2. **Type Safety**: Use appropriate flag types
3. **Default Values**: Provide sensible defaults
4. **Documentation**: Document flag purposes and effects

### Server Configuration

1. **Version Control**: Track flag configuration versions
2. **Rollout Plans**: Plan gradual rollouts
3. **Monitoring**: Monitor flag impact
4. **Rollback**: Prepare rollback procedures

### Client Implementation

1. **Fail-Safe**: Always fall back to defaults
2. **Caching**: Cache flags to reduce network calls
3. **Persistence**: Persist flags across app restarts
4. **Performance**: Minimize flag evaluation overhead

## Troubleshooting

### Common Issues

#### 1. Flags Not Updating
**Problem**: Flags not reflecting server changes
**Solution**: Check network connectivity and cache expiration

#### 2. Performance Issues
**Problem**: Flag evaluation is slow
**Solution**: Check flag evaluation frequency and caching

#### 3. Incorrect Values
**Problem**: Flags have wrong values
**Solution**: Verify server response and flag parsing

### Debug Mode

Enable debug logging to troubleshoot flag issues:

```kotlin
val config = PulseKitConfig {
    enableDebugLogging = true
}
PulseKitAndroid.initialize(context, config)
```

### Flag Validation

Validate flag values before applying:

```kotlin
fun validateFlagValue(flag: FeatureFlag, value: FlagValue): Boolean {
    return when (flag.type) {
        FlagType.INTEGER -> when (flag.key) {
            "event_batch_size" -> (value as? FlagValue.IntegerValue)?.value?.let { it > 0 } ?: false
            "session_timeout_minutes" -> (value as? FlagValue.IntegerValue)?.value?.let { it > 0 } ?: false
            else -> true
        }
        FlagType.BOOLEAN -> value is FlagValue.BooleanValue
        FlagType.DOUBLE -> value is FlagValue.DoubleValue
        FlagType.STRING -> value is FlagValue.StringValue
    }
}
```

## Security Considerations

### Data Privacy

- **No PII**: Flags never contain personally identifiable information
- **Metadata Only**: Flag values are metadata only
- **Secure Transport**: Use HTTPS for flag fetching
- **Validation**: Validate server responses

### Access Control

- **Authentication**: Require authentication for flag API
- **Authorization**: Limit flag changes to authorized users
- **Audit Trail**: Track flag changes and rollouts
- **Rate Limiting**: Prevent abuse of flag API

## Future Enhancements

### Planned Features

- **User Segmentation**: Target specific user segments
- **A/B Testing**: Built-in A/B testing framework
- **Conditional Flags**: Flags based on device/app characteristics
- **Real-time Updates**: WebSocket-based flag updates
- **Analytics Integration**: Flag usage analytics

### Tooling

- **Flag Dashboard**: Web dashboard for flag management
- **Rollout Automation**: Automated rollout tools
- **Impact Analysis**: Tools to analyze flag impact
- **Testing Framework**: Framework for testing flag changes

---

This feature flag system provides powerful remote control capabilities while maintaining the SDK's commitment to backward compatibility and developer experience.
