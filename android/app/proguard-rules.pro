# Keep Moshi generated adapters
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keep class com.phishdb.app.data.model.** { *; }

# Room
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Moshi
-keepclasseswithmembers class * { @com.squareup.moshi.* <methods>; }
-keep @com.squareup.moshi.JsonQualifier interface *
-keepclassmembers class kotlin.Metadata { public <methods>; }

# OkHttp platform
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
