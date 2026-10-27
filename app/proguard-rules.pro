# Reflection-based Moshi adapters for the generated OpenAPI models.
-keep class org.openapitools.client.models.** { *; }
-keepclassmembers class org.openapitools.client.models.** { *; }
-dontwarn org.openapitools.client.**

# Moshi + Kotlin metadata (the app uses KotlinJsonAdapterFactory).
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier @interface *
-keepclassmembers @com.squareup.moshi.JsonClass class * extends java.lang.Enum {
    <fields>;
}

# Room entities and generated database code.
-keep class com.tuxy.airo.data.flightdata.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Libraries that use reflection or are referenced from XML.
-keep class de.raphaelebner.roomdatabasebackup.** { *; }
-keep class com.jamal.composeprefs3.** { *; }
-keep class org.maplibre.android.** { *; }
-dontwarn org.maplibre.android.**

# Keep line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
