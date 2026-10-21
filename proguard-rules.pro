# ProGuard Configuration for PixelPlayer Desktop
# Goal: Shrink unused classes (especially material-icons-extended) safely without breaking reflection or JNI.

-dontobfuscate
-dontwarn **
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# Entry Point
-keep class MainKt {
    public static void main(java.lang.String[]);
}

# Kotlin Standard Library & Coroutines
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }

# libmpv & JNA (Native Audio Engine)
-keep class com.kayoi.pixelplayerdesktop.data.player.mpv.** { *; }
-keep interface com.kayoi.pixelplayerdesktop.data.player.mpv.** { *; }
-keep class com.sun.jna.** { *; }
-keep interface com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.Structure { <fields>; }

# SQLite JDBC & SQL
-keep class org.sqlite.** { *; }
-keep class java.sql.** { *; }

# Koin Dependency Injection
-keep class org.koin.** { *; }
-keep class io.insert.koin.** { *; }

# Kotlinx Serialization
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class * implements kotlinx.serialization.KSerializer { *; }
-keep @kotlinx.serialization.Serializable class * { *; }

# Audio Tagging & Formats
-keep class org.jaudiotagger.** { *; }
-keep class org.gagravarr.** { *; }

# Lyrics Romanization & Language Tools
-keep class com.atilika.kuromoji.** { *; }
-keep class com.belerweb.pinyin4j.** { *; }
-keep class net.sourceforge.pinyin4j.** { *; }
-keep class com.ibm.icu.** { *; }

# Dynamic Theming & File Picker
-keep class com.materialkolor.** { *; }
-keep class io.github.vinceglb.filekit.** { *; }

# PixelPlayer Models & Data State
-keep class com.kayoi.pixelplayerdesktop.data.model.** { *; }
-keep class com.kayoi.pixelplayerdesktop.data.preferences.** { *; }
-keep class com.kayoi.pixelplayerdesktop.presentation.viewmodel.** { *; }
