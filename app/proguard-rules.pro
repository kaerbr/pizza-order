# Rules for the R8-optimized release build (app/build.gradle.kts).
# kotlinx.serialization and Compose bring their own rules; only what they don't cover is here.

# Bluetooth printer library: keep it whole, so no class it looks up at runtime gets stripped.
-keep class com.dantsu.escposprinter.** { *; }

# Readable stack traces from the release build.
-keepattributes SourceFile,LineNumberTable
