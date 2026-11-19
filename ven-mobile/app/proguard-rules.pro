# Aturan R8 untuk build release VEN.
# Catatan: isMinifyEnabled masih false; aturan ini disiapkan agar saat
# minify diaktifkan, Retrofit/Gson/Unity tetap berfungsi (lihat STATUS doc).

# ---- Unity ----
# UnityBridge diakses via JNI dari Unity PlayBridge memakai nama string
# (com.yourapp.exhibition.UnityBridge), tidak boleh di-rename/dihapus.
-keep class com.yourapp.exhibition.UnityBridge { *; }
-keep class com.unity3d.player.** { *; }

# ---- Gson ----
# Model REST dipetakan lewat refleksi; nama field harus dipertahankan.
-keep class com.ven.app.data.api.models.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepattributes Signature
-keepattributes *Annotation*

# ---- Retrofit ----
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---- Coroutines ----
-dontwarn kotlinx.coroutines.**
