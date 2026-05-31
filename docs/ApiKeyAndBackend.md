# API Key and Backend Configuration

This guide explains how to obtain, configure, and securely use the PulseKit API key, and how the SDK communicates with your backend.

## What is the API Key?

The API key is an authentication token that identifies your app or project to the PulseKit backend. The SDK sends it with every batch request in the `Authorization: Bearer <api_key>` header. Your backend validates this key and routes events to the correct project.

## Backend Endpoint

The SDK sends event batches via HTTP POST to:

```
{baseUrl}/v1/events
```

- **Default baseUrl:** `https://api.pulsekit.dev`
- **Configurable:** Set `baseUrl` in `PulseKitConfig` to point to your own backend
- **Request:** `POST` with `Content-Type: application/json` and `Authorization: Bearer <api_key>`

## How to Get an API Key

1. Sign up at your PulseKit dashboard (e.g., `dashboard.pulsekit.dev`)
2. Create a new project or app
3. Copy the API key (e.g., `pk_live_abc123...`) from the project settings

## Production: Secure API Key Management

**Never hardcode the API key in source code** for production builds.

### Option 1: BuildConfig + local.properties

Add to your app's `build.gradle.kts`:

```kotlin
val localProperties = java.util.Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val pulsekitApiKey = localProperties.getProperty("pulsekitApiKey") ?: ""

android {
    defaultConfig {
        buildConfigField("String", "PULSEKIT_API_KEY", "\"$pulsekitApiKey\"")
    }
}
```

Add to `local.properties` (this file is gitignored):

```
pulsekitApiKey=pk_live_your_actual_key
```

Use in code:

```kotlin
apiKey = BuildConfig.PULSEKIT_API_KEY
```

### Option 2: Environment Variable

Pass at build time via `-PpulsekitApiKey=xxx` or set in CI secrets.

### Option 3: Secrets Gradle Plugin

Use [com.google.android.libraries.mapsplatform.secrets-gradle-plugin](https://github.com/google/secrets-gradle-plugin) to load from a `secrets.properties` file.

## Demo and Test

For development, demos, or tests:

- Use `demo-api-key` as a placeholder (your backend may reject or accept it)
- Enable `enableDebugLogging = true` to inspect network requests

## Sample App

The sample app uses BuildConfig with fallback to `demo-api-key`. Configure your key in `local.properties`:

```
pulsekitApiKey=your_key_here
```
