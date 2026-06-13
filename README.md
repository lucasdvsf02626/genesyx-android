# Genesyx — Native Android (Kotlin + Compose)

Native rebuild of the Genesyx fertility-prep app. The web app (repo root) stays live;
this is the from-scratch native client. See the specs in [`../docs`](../docs) and
[`../ARCHITECTURE.md`](../ARCHITECTURE.md).

## Stack
Jetpack Compose + Material 3 · single-Activity · MVVM / Clean Architecture · Hilt ·
Room (offline cache) · DataStore · supabase-kt (auth + Postgrest) · Navigation Compose.
`compile/targetSdk 35`, `minSdk 26`, JDK 17, package `com.genesyx.app`.

## First-time setup
The Gradle wrapper JAR isn't committed. Generate it once (or just open the project in
Android Studio, which does it for you):

```bash
cd android
gradle wrapper --gradle-version 8.9   # requires a local Gradle; or use Android Studio
```

### Supabase credentials
Set these in `android/local.properties` (git-ignored) or as CI gradle properties:

```properties
genesyx.supabaseUrl=https://<project>.supabase.co
genesyx.supabaseAnonKey=<anon-public-key>
```

They're exposed to the app as `BuildConfig.SUPABASE_URL` / `SUPABASE_ANON_KEY`.

## Build & run
```bash
./gradlew assembleDebug          # build the debug APK
./gradlew testDebugUnitTest      # run unit tests (incl. CycleEngineTest)
./gradlew installDebug           # install on a connected device/emulator (API 26+)
```

## Current status (foundation)
Scaffolded: Gradle + version catalog, manifest (incl. invite deep links), theme
(light + dark, matched to the web tokens), navigation graph (all 14 destinations),
MainActivity + bottom nav, the **cycle engine** (ported verbatim, unit-tested), the
Home screen, and a Supabase Hilt module. All other screens are placeholders, built
out one at a time per [`../docs/SCREEN_LAYOUTS.md`](../docs/SCREEN_LAYOUTS.md).

## Next
Data layer (Supabase repositories + Room cache), then screens in order: Home → onboarding
→ Track → Nutrition → Insights → pH → Log → Profile → Auth → Invite → Pregnancy.
Privileged ops (partner accept/unlink, account delete) → Supabase Edge Functions.
Fonts (Outfit + Inter) and brand assets (eggs, logo) still to be bundled.
