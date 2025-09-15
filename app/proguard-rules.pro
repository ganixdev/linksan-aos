# LinkSan ProGuard rules for release optimization

# Keep main application classes
-keep class com.ganixdev.linksan.MainActivity { *; }
-keep class com.ganixdev.linksan.ProcessTextActivity { *; }

# Keep data classes used for JSON parsing
-keep class com.ganixdev.linksan.URLProcessor$ProcessingResult { *; }
-keep class com.ganixdev.linksan.URLSanitizer$DomainRules { *; }
-keep class com.ganixdev.linksan.URLSanitizer$RedirectHandler { *; }

# Keep classes that use reflection or are called from XML/manifests
-keepclassmembers class * extends android.app.Activity {
    public void onCreate(android.os.Bundle);
}

# Keep JSON-related functionality
-keepattributes Signature
-keepattributes *Annotation*
-keep class org.json.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# General Android optimizations
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Optimization settings
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Remove debug logs in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile