# Keep kotlinx-serialization generated serializers
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class com.linehu.asi.** {
    kotlinx.serialization.KSerializer serializer(...);
}
