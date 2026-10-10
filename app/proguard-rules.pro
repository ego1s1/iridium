# Iridium ProGuard/R8 rules (release minification enabled; verified on device).
#
# Most libraries ship their own consumer rules (Hilt, Room, Coil,
# kotlinx-serialization, desugar); only Readium needs explicit keeps because
# the streamer/parser touch model classes reflectively across modules.

# Readium toolkit: publication model + parsers reached via reflection.
-keep class org.readium.** { *; }
-keepnames class org.readium.** { *; }

# kotlinx-serialization: keep serializers referenced only from Companion.
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Native bridge: called from C++ by name.
-keepclasseswithmembernames class * {
    native <methods>;
}

# Crash reports stay symbolicated through mapping uploads (CI).
-keepattributes SourceFile,LineNumberTable
