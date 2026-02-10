# Consumer ProGuard rules for PulseKit Android SDK.
# These rules are merged into the app's ProGuard config when the app depends on this library.
# See: pulsekit-android/Module.md

# Keep PulseKit public API so SDK works when the app is minified
-keep class com.pulsekit.** { *; }
-keep class com.pulsekit.android.** { *; }

# Kotlin serialization (events, config)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-dontnote kotlinx.serialization.SerializationKt

# SQLite (used by storage layer)
-keep class android.database.sqlite.** { *; }
