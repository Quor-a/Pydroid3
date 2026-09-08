# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Python engine
-keep class com.pydroid.interpreter.engine.** { *; }
-keep class com.pydroid.interpreter.engine.PythonEngine {
    native <methods>;
}

# Keep JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}
