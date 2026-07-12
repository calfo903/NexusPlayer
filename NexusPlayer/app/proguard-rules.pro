# ProGuard rules for Nexus Player
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-keep class com.nexusplayer.app.domain.model.** { *; }
-keep class com.nexusplayer.app.data.remote.model.** { *; }
-dontwarn androidx.media3.**
-dontwarn com.google.android.gms.cast.**
