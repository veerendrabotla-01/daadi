# Add custom proguard rules here to prevent over-optimization of serialized models or library reflection issues.
# Moshi Rules
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Retrofit Rules
-keepattributes Signature, InnerClasses, EnclosingMethod, AnnotationDefault, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep our data model and state classes intact to prevent deserialization failures
-keep class com.example.daadi.data.** { *; }
-keep class com.example.daadi.model.** { *; }
-keep class com.example.daadi.engine.** { *; }

# Room Rules
-keep class * extends androidx.room.RoomDatabase
-keep class * implements androidx.room.RoomDatabase
-keep interface * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Play Integrity and AdMob Rules
-keep class com.google.android.play.core.integrity.** { *; }
-keep class com.google.android.gms.ads.** { *; }

# Serialization Rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class **$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class * implements kotlinx.serialization.KSerializer { *; }
-keepclassmembers class * implements kotlinx.serialization.KSerializer { *; }
-keepclassmembers class * implements kotlinx.serialization.internal.GeneratedSerializer { *; }

# General OkHttp / Retrofit platform keep
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
