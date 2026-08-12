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

# ── Google sign-in: Credential Manager + Google Identity ────────────────────────────────
# R8 full mode (default on AGP 8+) can strip/rename the credential classes whose data is read
# reflectively from the returned Bundle (GoogleIdTokenCredential.createFrom), which fails only on
# the minified release build — "Google sign-in works in debug, does nothing in release". These libs
# ship consumer rules, but keep them explicitly as belt-and-braces. See the 12 Aug Supabase auth
# audit (§4 Android, proguard row).
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**
