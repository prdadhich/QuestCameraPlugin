# Keep Camera2 plugin classes
-keep class com.stunad.questcamera.SimpleCamera { *; }
-keep class com.stunad.questcamera.Camera2Helper { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}