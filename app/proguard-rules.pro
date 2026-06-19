# Genesyx R8/ProGuard rules. Minification + resource shrinking are enabled for `release`
# (see build.gradle.kts). These keeps prevent R8 from stripping reflection-driven code that
# only fails at runtime on the minified build.

# ── kotlinx.serialization (Supabase DTOs) ───────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.**
# Keep generated serializers and @Serializable companions.
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-keep,includedescriptorclasses class kotlinx.serialization.** { *; }

# ── Supabase-kt + Ktor + coroutines ─────────────────────────────────────────────────────
-keep class io.github.jan.supabase.** { *; }
-keepclassmembers class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-dontwarn io.ktor.**
-keepnames class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Optional logging/transport backends pulled in transitively.
-dontwarn org.slf4j.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Hilt / Dagger ───────────────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
-keepclasseswithmembernames class * { @javax.inject.Inject <init>(...); }

# ── Room ────────────────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ── App models / DTOs (serialized now or later) ─────────────────────────────────────────
-keep class com.genesyx.app.domain.model.** { *; }
-keep class com.genesyx.app.data.** { *; }

# Compose is R8-safe out of the box; keep enum values used via valueOf/entries reflection.
-keepclassmembers enum * { *; }
