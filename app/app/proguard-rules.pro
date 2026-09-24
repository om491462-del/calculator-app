# ProGuard rules for Calculator app

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep app classes
-keep class com.lab.calculator.** { *; }