# ProGuard Rules for Scale Finder App
# For more details: https://developer.android.com/guide/developing/tools/proguard.html

# ============================================================================
# ATTRIBUTES - Keep important metadata
# ============================================================================
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeInvisibleParameterAnnotations

# ============================================================================
# KOTLIN SPECIFIC
# ============================================================================
# Keep Kotlin metadata for reflection and serialization
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.reflect.jvm.internal.**

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Kotlin coroutines
-keep class kotlin.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep Kotlin collections
-keep class kotlin.** { *; }

# ============================================================================
# ANDROIDX / JETPACK COMPOSE
# ============================================================================
# Keep Compose classes
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }

# Keep Compose Material
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.material3.** { *; }

# Keep lifecycle classes
-keep class androidx.lifecycle.** { *; }

# Keep ViewModels (used with Compose)
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep activity classes
-keep class androidx.activity.** { *; }

# Keep fragment classes  
-keep public class * extends androidx.fragment.app.Fragment

# ============================================================================
# APP-SPECIFIC CLASSES
# ============================================================================

# Keep all model classes (Note, Scale, Chord, etc.)
-keep class com.lddev.scalefinder.model.** { *; }

# Keep audio classes
-keep class com.lddev.scalefinder.audio.** { *; }

# Keep UI composables
-keep class com.lddev.scalefinder.ui.** { *; }

# Keep MainActivity and its methods
-keep class com.lddev.scalefinder.MainActivity { *; }

# ============================================================================
# REFLECTION / SERIALIZATION
# ============================================================================
# Keep classes used with reflection (like data classes in Compose)
-keep class com.lddev.scalefinder.model.** {
    <init>(...);
    <fields>;
    <methods>;
}

# Keep Companion objects for sealed classes and enums
-keepclassmembers class * {
    @kotlin.jvm.JvmStatic *;
}

# ============================================================================
# ANDROID SYSTEM CLASSES
# ============================================================================
# Keep Android Parcelables
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep View constructors
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ============================================================================
# COROUTINES
# ============================================================================
-keepclassmembernames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepclassmembernames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames interface kotlinx.coroutines.CoroutineExceptionHandler {}

# ============================================================================
# WARNINGS
# ============================================================================
# Suppress warnings for missing classes
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn androidx.**
-dontwarn com.google.**

# ============================================================================
# R8 OPTIMIZATIONS
# ============================================================================
# Allow aggressive optimizations
-allowaccessmodification
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5