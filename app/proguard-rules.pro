# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.reflect.jvm.internal.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Jetpack Compose
-keep class androidx.compose.** { *; }
-keep class kotlin.coroutines.** { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }

# Keep ViewModels for Compose
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep data classes used in Compose
-keep class com.lddev.scalefinder.model.** { *; }

# Keep Composable functions
-keep @androidx.compose.runtime.Composable class * { *; }