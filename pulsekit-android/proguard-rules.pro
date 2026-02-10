# ProGuard rules for PulseKit Android library release build.
# Used when this module is built with minifyEnabled = true (e.g. for release).
# See: pulsekit-android/Module.md

# Keep PulseKit classes
-keep class com.pulsekit.** { *; }
-keep class com.pulsekit.android.** { *; }

# Keep serialization classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-dontnote kotlinx.serialization.SerializationKt

# Keep SQLite classes
-keep class android.database.sqlite.** { *; }
