# ========================================
# ProGuard/R8 Rules for NeoCalc
# ========================================

# ----------------------------------------
# UniFFI Generated Bindings
# ----------------------------------------
# Keep all classes in the uniffi generated package (used by Rust FFI)
-keep class uniffi.** { *; }
-keepclassmembers class uniffi.** { *; }

# Keep the JNA library classes used by UniFFI
-keep class com.sun.jna.** { *; }
-keepclassmembers class com.sun.jna.** { *; }

# Keep native method names
-keepclasseswithmembernames class * {
    native <methods>;
}

# ----------------------------------------
# Kotlin Serialization (if used)
# ----------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ----------------------------------------
# Compose
# ----------------------------------------
# Keep Compose classes for reflection
-keep class androidx.compose.** { *; }

# ----------------------------------------
# Coroutines
# ----------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ----------------------------------------
# General
# ----------------------------------------
# Keep source file names and line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable

# Keep annotations
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Disable obfuscation warnings for missing classes (release builds only)
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
