# v1.1 — Push Notifications + Learn Section

**Status:** Design brief. Not built. No source edits made.
**Author:** drafted 2026-07-09 against `fix/app-icon` @ `49d07f2`.
**Scope:** Android only — Kotlin + Jetpack Compose + Material 3, single-activity Navigation Compose.

> **Read before building.** This brief is grounded in the code as it exists today, not in `CLAUDE.md`.
> `CLAUDE.md` is stale in three ways that matter here — see [§0 Preflight](#0-preflight-what-the-repo-actually-looks-like).

---

## 0. Preflight — what the repo actually looks like

Three corrections to `CLAUDE.md`, verified in source. Fix these before or alongside v1.1 work, because two of them change what you build.

| Claim in `CLAUDE.md` | Reality | Impact |
|---|---|---|
| "CODE FROZEN at versionCode 6" | `app/build.gradle.kts:38` → `versionCode = 7` | Docs only. Freeze has clearly lifted; several `fix/*` branches landed after. |
| "pH is LOCAL-ONLY, no network call fires, `ph_readings` table does not exist" | `PhRepository.kt:100` calls `remote.upsert(...)`; a full sync queue exists (`PhSyncStatus`, `syncPending()`, `PhSyncWorker`, `PhSyncScheduler`); `docs/schema.sql:102` has `CREATE TABLE public.ph_readings` | **The headline v1.1 backlog item is already done.** The pH sync work landed via `feature/v1.1-sync-hardening`. |
| `FeatureFlags.kt:6-11` — "fires no network call for pH (see its 'v1.1' guards)" | `grep -n "v1.1" PhRepository.kt` → **zero hits**. The guards are gone. | **The KDoc lies.** Anyone reading `FeatureFlags` will make a wrong decision. Fix the comment. |

Also true and load-bearing for this brief:

- **`minSdk = 26`.** Notification channels are mandatory on every device we support — no `if (SDK_INT >= O)` guard anywhere. `POST_NOTIFICATIONS` *is* version-gated (API 33+).
- **WorkManager is already a dependency** (`libs.androidx.work.runtime.ktx`) with an established pattern to copy — see [§2.3](#23-scheduling-copy-the-phsyncscheduler-pattern).
- **`push_enabled` already exists** in DataStore (`GenesyxPreferencesDataStore.kt:39`, default `true`) and nothing reads it. `FeatureFlags.PUSH_NOTIFICATIONS = false` correctly hides the orphaned toggle.
- **Bottom nav is full.** `Screen.bottomTabs` = 5 items; Material 3 `NavigationBar` is specified for 3–5. Learn cannot be a sixth tab.
- **A Learn stub already ships.** `NutritionContent.kt:138` defines `data class Article(val title: String, val read: String)` — and `NutritionScreen.kt:154-173` opens *all three* tiles into the same hardcoded paragraph. This is the single most visible thing v1.1 fixes.
- **Account deletion calls `database.clearAllTables()`** (`AuthRepository.kt:58`). Anything user-generated that we put in Room is wiped for free. Anything we put in DataStore is not.

---

## 1. Android feature overview

Two features, one thesis: **the app currently asks users to remember to log, and gives them nowhere to learn why it matters.** Notifications close the first gap. Learn closes the second. They connect: every article ends in a CTA back into a tracked action, and every reminder can deep-link into an article that explains the ask.

### Feature 1 — Reminders (not "push")

Call it **Reminders**, not Push Notifications, in all user-facing copy.

This is deliberate and it changes the architecture. Everything in scope is **locally scheduled** — WorkManager + `NotificationManagerCompat`. There is no FCM, no Firebase, no server-sent push, no device token, no `google-services.json`.

Why this is the right call for v1.1:

- Every reminder we want (log your day, drink water, weekly insights, you've been away) is a **function of local state and the clock**. The device already knows. A server round-trip adds nothing.
- No FCM means no new Play Data Safety disclosure for device identifiers, no token lifecycle, no server infrastructure, no Firebase project to keep in sync with the existing Supabase + Google Cloud setup.
- It is strictly additive. If v1.2 wants server-initiated campaigns, FCM slots in behind the same `Reminder` domain model.

If someone later says "we need push for marketing" — that is a different feature with a different privacy posture. Do not smuggle it in here.

**Two reminder families, per the brief:**

1. **Tracking reminders** — log symptoms, log your day, you missed yesterday, weekly insights are ready.
2. **Nutrition & wellness reminders** — hydration, meal consistency, phase-aware nutrition nudges.

### Feature 2 — Learn

A first-class educational section: landing screen, categories, search, article detail, bookmarks, share, related articles, reading time, CTAs back into Track/Nutrition/Insights.

**Content ships bundled in the APK.** Decision and rationale in [§5.1](#51-where-content-lives-and-why-its-not-in-supabase).

### What this brief does *not* cover

- FCM / server-sent push.
- A CMS or authoring pipeline.
- Notification A/B testing or campaign tooling.
- Rich-media articles (video, embedded interactive charts). Text + one hero image per article.

---

## 2. Android push notification architecture

### 2.1 Module layout

New package `com.genesyx.app.notifications`, sibling to `data/sync`:

```
notifications/
  NotificationChannels.kt        // channel IDs + createAll(), called from GenesyxApplication
  NotificationPermission.kt      // POST_NOTIFICATIONS state machine (pure, testable)
  ReminderScheduler.kt           // interface + WorkManagerReminderScheduler  ← mirrors PhSyncScheduler
  ReminderWorker.kt              // CoroutineWorker, deps via Hilt EntryPoint  ← mirrors PhSyncWorker
  ReminderNotifier.kt            // builds + posts notifications, owns quiet hours + suppression
  ReminderDeepLinks.kt           // genesyx:// URI construction, single source of truth
  model/
    ReminderKind.kt              // enum: TRACKING, HYDRATION, NUTRITION, MISSED_LOG, WEEKLY_INSIGHTS, REENGAGEMENT
    NotificationSettings.kt      // the settings data class
data/
  NotificationSettingsRepository.kt   // DataStore-backed, sits beside PreferencesRepository
ui/settings/
  ReminderSettingsScreen.kt
  ReminderSettingsViewModel.kt
  PrePermissionSheet.kt
```

### 2.2 Notification channels

Channels are created **unconditionally** in `GenesyxApplication.onCreate()`. `minSdk = 26` means `NotificationChannel` exists on every supported device — no version guard. Creating an existing channel is a no-op, so this is safe on every launch.

Four channels. Resist the urge to make one per reminder type; users manage channels in system settings and six switches is a wall.

| Channel ID | User-visible name | Importance | Carries | Rationale |
|---|---|---|---|---|
| `tracking_reminders` | Tracking reminders | `IMPORTANCE_DEFAULT` | daily log nudge, missed-log | User explicitly opted into a daily habit; sound is appropriate. |
| `nutrition_wellness` | Nutrition & hydration | `IMPORTANCE_DEFAULT` | hydration, nutrition, meal consistency | Same — a silent hydration reminder is a useless hydration reminder. |
| `weekly_insights` | Weekly insights | `IMPORTANCE_LOW` | weekly insights ready | Informational, not time-critical. No sound. |
| `reengagement` | Occasional check-ins | `IMPORTANCE_LOW` | you've been away | **Must be `LOW` and separately disableable.** This is the channel users resent. Making it silent and easy to kill is what keeps them from disabling *all* notifications — or uninstalling. |

Set `setShowBadge(false)` on `weekly_insights` and `reengagement`.

**Do not** create a channel group unless a fifth channel appears. Four flat channels read fine in system settings.

```kotlin
object NotificationChannels {
    const val TRACKING = "tracking_reminders"
    const val NUTRITION = "nutrition_wellness"
    const val INSIGHTS = "weekly_insights"
    const val REENGAGEMENT = "reengagement"

    fun createAll(context: Context) {
        val mgr = NotificationManagerCompat.from(context)
        mgr.createNotificationChannelsCompat(
            listOf(
                channel(TRACKING, R.string.channel_tracking_name, IMPORTANCE_DEFAULT),
                channel(NUTRITION, R.string.channel_nutrition_name, IMPORTANCE_DEFAULT),
                channel(INSIGHTS, R.string.channel_insights_name, IMPORTANCE_LOW, badge = false),
                channel(REENGAGEMENT, R.string.channel_reengagement_name, IMPORTANCE_LOW, badge = false),
            )
        )
    }
}
```

Channel names and descriptions go in `strings.xml`. They are user-visible in system settings and must be localizable.

### 2.3 Scheduling — copy the `PhSyncScheduler` pattern

The repo already solved "schedule background work without making the repository untestable." Follow it exactly rather than inventing a second idiom.

`PhSyncScheduler.kt:16-18` is an interface so `PhRepository` stays a plain JVM unit test. `PhSyncWorker.kt:23-26` pulls its dependency through a Hilt `EntryPoint` because WorkManager constructs the worker itself. Mirror both.

```kotlin
/** Schedules reminder deliveries. Interface so ViewModels/repos stay JVM-testable. */
interface ReminderScheduler {
    fun rescheduleAll(settings: NotificationSettings)
    fun cancel(kind: ReminderKind)
    fun cancelAll()
}

@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReminderScheduler { /* ... */ }
```

**Use `OneTimeWorkRequest` with a computed `initialDelay`, not `PeriodicWorkRequest`.**

This is the single most important scheduling decision, and the obvious choice is the wrong one. `PeriodicWorkRequest` has a **15-minute minimum interval and no guarantee about *when* inside the period it fires**. A user who sets a 9:00 PM reminder will get it at some unpredictable point in a window. That is unacceptable for a reminder whose entire value is being at 9:00 PM.

Instead: on each run, the worker (a) posts today's notification if appropriate, then (b) computes the next occurrence from `NotificationSettings` and enqueues a fresh one-time request with that `initialDelay`. Self-rescheduling chain.

```kotlin
val next = nextOccurrence(settings, now = ZonedDateTime.now())
val request = OneTimeWorkRequestBuilder<ReminderWorker>()
    .setInitialDelay(Duration.between(ZonedDateTime.now(), next))
    .setInputData(workDataOf(KEY_KIND to kind.name))
    .build()
WorkManager.getInstance(context)
    .enqueueUniqueWork(kind.workName, ExistingWorkPolicy.REPLACE, request)
```

Note `ExistingWorkPolicy.REPLACE`, where `PhSyncScheduler` uses `KEEP`. Different semantics on purpose: pH wants "a drain is already queued, don't stack duplicates." Reminders want "settings changed, the old schedule is now wrong, throw it away."

**Constraints:** none. Do **not** set `setRequiredNetworkType(CONNECTED)`. A local reminder must fire in airplane mode. (Contrast `PhSyncScheduler.kt:27`, which correctly requires network — it's syncing.)

**Things that will silently break the chain, and the mitigations:**

| Hazard | Mitigation |
|---|---|
| Device reboot | WorkManager persists across reboot. Nothing needed. This is the main reason not to use `AlarmManager`. |
| OEM aggressive task-killing (Xiaomi, Huawei, OnePlus, Samsung) | Cannot be fully solved. Add a `RESCHEDULE_ALL` unique periodic worker (24h, `KEEP`) as a self-healing net that re-enqueues any missing chain. Accept some loss. |
| Timezone change / DST | `ReminderWorker` recomputes `nextOccurrence` from wall-clock local time on every run, so it self-corrects within one cycle. Also register `ACTION_TIMEZONE_CHANGED` → `rescheduleAll()`. |
| User changes reminder time | `rescheduleAll()` on every settings write. `REPLACE` handles it. |
| App upgrade | WorkManager survives. But bump a `schedule_version` int in DataStore and `rescheduleAll()` if it changed, so scheduling-logic fixes actually reach existing users. |
| Doze mode | `setInitialDelay` work runs in a maintenance window — may be minutes late. Acceptable for reminders. Do **not** reach for `setExactAndAllowWhileIdle`; the accuracy isn't worth the battery/permission cost. |

**Exact-alarm permissions (`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`) are out of scope and should stay that way.** Google Play restricts `USE_EXACT_ALARM` to alarm-clock and calendar apps. A wellness reminder does not qualify, and requesting it is a plausible review rejection. WorkManager's ±few-minutes accuracy is correct here.

### 2.4 Manifest additions

```xml
<!-- API 33+ runtime notification permission. Harmless on 26–32. -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Re-arm the reminder chain after reboot (self-healing belt-and-braces; WorkManager
     already persists, but OEM task-killers make this worth having). -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

And the activity needs `android:launchMode="singleTop"` — see [§2.6](#26-deep-linking-the-part-that-will-bite-you), which is the trap in this feature.

### 2.5 Runtime permission — the `POST_NOTIFICATIONS` state machine

On **API < 33** (our `minSdk = 26` floor through API 32) there is no runtime permission. Notifications are enabled unless the user turned them off in system settings. `NotificationManagerCompat.areNotificationsEnabled()` is the check on *every* API level and is what the settings screen should read — not `checkSelfPermission`.

On **API 33+**, `POST_NOTIFICATIONS` is a runtime permission with all of `shouldShowRequestPermissionRationale`'s usual sharp edges.

```kotlin
enum class PushPermissionStatus {
    NOT_REQUIRED,   // API < 33 and notifications enabled
    GRANTED,
    DENIED_SOFT,    // denied once; system will show the dialog again
    DENIED_PERMANENT, // denied twice, or "Don't allow" on 33+ — system dialog will no-op forever
    BLOCKED_IN_SETTINGS, // permission granted but areNotificationsEnabled() == false
    NOT_ASKED,
}
```

The critical, near-universally-botched detail: **on Android 13+ the system permission dialog only ever shows twice.** After a second denial, calling `requestPermissionLauncher.launch(...)` does nothing at all — no dialog, no callback distinguishing it from an instant denial. If the app's only path to enabling reminders is that button, the feature is permanently unreachable and the user believes the app is broken.

So:

- **Never call `launch()` cold.** Always show the pre-permission sheet first ([§3.1](#31-pre-permission-explanation)).
- Detect `DENIED_PERMANENT` by checking `!shouldShowRequestPermissionRationale()` *after* at least one recorded denial (`lastPromptedAt != null` in settings). The `shouldShowRationale == false && neverAsked` case is indistinguishable from `DENIED_PERMANENT` by the framework alone — this is exactly why `lastPromptedAt` is persisted.
- In `DENIED_PERMANENT` or `BLOCKED_IN_SETTINGS`, the button must stop saying "Enable" and start saying **"Open settings"**, routing to:

```kotlin
Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
```

`ACTION_APP_NOTIFICATION_SETTINGS` (API 26+) lands on the notification page directly. `ACTION_APPLICATION_DETAILS_SETTINGS` is the fallback if the former throws `ActivityNotFoundException` on an odd OEM ROM. Wrap in `runCatching`.

- Re-check `areNotificationsEnabled()` in `onResume` / a `LifecycleEventObserver` so returning from system settings updates the UI immediately. Without this the screen lies until the user backs out and returns.

**When to ask.** Not on first launch. The permission is requested at the moment the user turns on their first reminder toggle in Reminder Settings — the one moment the ask is self-evidently justified. That single rule replaces "pre-permission priming screen in onboarding," which would be a worse product and a worse conversion rate.

### 2.6 Deep linking — the part that will bite you

Notification taps must open the right screen. The repo has a working deep-link precedent (`GenesyxNavGraph.kt:115-134`, `genesyx://invite/{code}`), so extend the same scheme.

| `ReminderKind` | Deep link URI | Lands on |
|---|---|---|
| `TRACKING` | `genesyx://log` | `Screen.Log` |
| `MISSED_LOG` | `genesyx://log?date={iso}` | `Screen.Log`, prefilled to that date |
| `HYDRATION` | `genesyx://track` | `Screen.Track` |
| `NUTRITION` | `genesyx://nutrition` | `Screen.Nutrition` |
| `WEEKLY_INSIGHTS` | `genesyx://insights` | `Screen.Insights` |
| `REENGAGEMENT` | `genesyx://home` | `Screen.Home` |
| (Learn, from CTAs/share) | `genesyx://learn/article/{slug}` | `Screen.ArticleDetail` |

Add one manifest intent-filter per host, or a single filter with multiple `<data>` elements sharing `android:scheme="genesyx"`.

**Three traps, in ascending order of how long they'll cost you:**

**(a) `MainActivity` has no `launchMode`.** It defaults to `standard`. Tapping a notification while the app is already foregrounded creates a *second* `MainActivity` instance on top of the first. Two nav graphs, two `AppViewModel`s, back button pops into a zombie. Set `android:launchMode="singleTop"` and override:

```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    navController.handleDeepLink(intent)   // requires hoisting navController out of setContent
}
```

Hoisting `navController` to an activity field is a real change to `MainActivity.kt` — currently it lives inside `setContent` via `rememberNavController()`. Expose it through a `lateinit var` or a `MutableState<NavHostController?>` assigned on composition.

**(b) The NavHost is built conditionally.** `MainActivity.kt` does `if (route != null) { ... GenesyxNavGraph(...) }`, gated on `AppViewModel.startRoute` resolving from DataStore. The activity `Intent` exists before the NavHost does. Navigation Compose consumes the intent when the `NavHost` composes, so the happy path works — but only because `startRoute` resolves fast. Verify explicitly with a cold-start-from-notification test. If it proves flaky, stash the deep-link `Uri` in `AppViewModel` and `navigate()` once `startRoute` emits.

**(c) A notification must never deep-link a signed-out user past the auth gate.** `49d07f2` deliberately gated the dashboard behind register/login. `ReminderNotifier` must therefore refuse to post *any* reminder when `SessionRepository.awaitSignedIn()` is false, and `cancelAll()` must run on sign-out (`AuthRepository`). Cheaper and safer than handling the tap-side redirect.

Build `PendingIntent`s with `NavDeepLinkBuilder`, or:

```kotlin
val intent = Intent(Intent.ACTION_VIEW, uri, context, MainActivity::class.java)
PendingIntent.getActivity(
    context, kind.ordinal, intent,
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)
```

`FLAG_IMMUTABLE` is **mandatory** on API 31+ (targetSdk 35 — the app will crash without it). Use a distinct `requestCode` per `ReminderKind`, or `FLAG_UPDATE_CURRENT` will mutate the wrong pending intent.

### 2.7 Quiet hours, suppression, and not being annoying

All enforcement lives in one place — `ReminderNotifier.shouldPost(kind, now, settings, state)` — as a **pure function**, unit-tested. Do not scatter these checks across workers.

Rules, in evaluation order (first match wins, notification suppressed):

1. **Not signed in** → suppress. (See trap (c) above.)
2. **Notifications disabled at OS level** (`!areNotificationsEnabled()`) → suppress and don't reschedule; nothing to do.
3. **Kind's own toggle is off** → suppress.
4. **Quiet hours** → if `quietHoursEnabled` and `now` falls inside `[quietHoursStart, quietHoursEnd)`, suppress. Must handle the **overnight wrap** (22:00 → 07:00 spans midnight; a naive `start <= now && now < end` fails). Suppress, don't defer — a hydration reminder at 07:00 that was meant for 23:00 is noise.
5. **Day-of-week not selected** → suppress.
6. **Action already taken today** → if the user already logged today, suppress `TRACKING` and `MISSED_LOG`. Query `DailyLogDao` at notify time. *This is the highest-value rule in the list and the one most often skipped.* Nothing burns trust faster than "Time to log your day!" ninety seconds after logging your day.
7. **Global daily cap: 2 notifications/day**, across all channels. `REENGAGEMENT` never counts toward the cap because it can't co-occur (rule 8).
8. **Re-engagement gating** → only if `daysSinceLastOpen >= 3`; at most **once every 7 days**; hard stop after **3 consecutive unacknowledged** re-engagement notifications. A user who ignored three is telling you something. Stop.
9. **Post-permission grace** → no notification within 24h of first grant except the one the user just scheduled. Prevents an accidental burst on day one.

Counters (`notificationsPostedToday`, `lastReengagementAt`, `consecutiveIgnoredReengagements`, `lastOpenedAt`) live in DataStore.

`lastOpenedAt` is written in `MainActivity.onStart()`. "Acknowledged" = app opened within 24h of a re-engagement post — approximate, and good enough; do not build tap-attribution plumbing for this.

---

## 3. Android notification UX flows

### 3.1 Pre-permission explanation

Not a full screen, and **not part of onboarding**. A Material 3 `ModalBottomSheet`, shown exactly once, at the moment the user flips their first reminder toggle.

Why a sheet and not a screen: the user just expressed intent by tapping a toggle. A full-screen interstitial punishes that intent. The sheet keeps the toggle visible behind it, so the ask is obviously about *the thing they just touched*.

```
┌────────────────────────────────┐
│              🔔                │
│                                │
│   Reminders that fit your day  │
│                                │
│   Genesyx will send a gentle   │
│   nudge at the time you pick.  │
│   You choose which reminders,  │
│   when, and how often — and    │
│   you can turn them off any    │
│   time.                        │
│                                │
│   Nothing leaves your device.  │
│   Reminders are scheduled      │
│   locally.                     │
│                                │
│  ┌──────────────────────────┐  │
│  │      Allow reminders     │  │  → requestPermission()
│  └──────────────────────────┘  │
│                                │
│         Not right now          │  → dismiss, toggle reverts to off
└────────────────────────────────┘
```

"Nothing leaves your device" is **literally true** for local scheduling and is the strongest line in the sheet. It is also a promise: if v1.2 adds FCM, this copy must change.

Set `lastPromptedAt = now` when "Allow reminders" is tapped. That timestamp is what disambiguates `NOT_ASKED` from `DENIED_PERMANENT` later ([§2.5](#25-runtime-permission--the-post_notifications-state-machine)).

### 3.2 Permission outcome states

| Outcome | Toggle | Inline message | Action |
|---|---|---|---|
| Granted | Turns on, time picker expands | — | Schedule. Show a `Snackbar`: "Reminder set for 9:00 PM." |
| Denied (soft, 1st) | Reverts to off | — | Silent. No nag. Next tap re-shows the system dialog. |
| Denied (permanent) | Reverts to off, section dims | "Reminders are turned off for Genesyx." | Button text → **"Open settings"**. |
| Postponed ("Not right now") | Reverts to off | — | Silent. Sheet can show again on next toggle tap. |
| Granted, then blocked in system settings | Toggles read on but section shows a banner | "Notifications are off in your phone's settings. Turn them on to get reminders." | **"Open settings"** |

The blocked-in-settings banner needs `areNotificationsEnabled()` re-checked on resume, or it will be stale and wrong. This is the state users actually land in and the one that's usually unhandled.

### 3.3 Reminder settings screen

Route: `Screen.ReminderSettings` (`"reminder_settings"`). Reached from **Profile → Reminders**. Add to `Screen.noBottomNavRoutes` — it's a modal-ish settings destination, matching `LogHistory` and `Pregnancy`.

The existing Profile toggle at `ProfileScreen.kt:199` (`SwitchRow("Push Notifications", push)`) becomes a **navigation row**, not a switch. A single master switch cannot express per-category schedules. Keep `push_enabled` as the master kill switch behind it.

```
┌─────────────────────────────────────┐
│  ←   Reminders                      │   CenterAlignedTopAppBar
├─────────────────────────────────────┤
│                                     │
│  ⚠  Notifications are off in your   │   ← conditional banner
│     phone's settings.               │
│     [ Open settings ]               │
│                                     │
│  TRACKING                           │   ← section header, labelMedium, onSurfaceVariant
│  ┌─────────────────────────────────┐│
│  │ Daily log reminder        [ ●] ││
│  │ A nudge to log how you feel     ││   supportingContent
│  ├─────────────────────────────────┤│
│  │ Reminder time        9:00 PM  › ││   ← visible only when toggle on
│  ├─────────────────────────────────┤│
│  │ Repeat        M T W T F S S     ││   ← day chips
│  ├─────────────────────────────────┤│
│  │ Missed-log nudge          [ ●] ││
│  │ If you forget, we'll check in   ││
│  │ the next morning                ││
│  └─────────────────────────────────┘│
│                                     │
│  NUTRITION & WELLNESS               │
│  ┌─────────────────────────────────┐│
│  │ Hydration                 [○ ] ││
│  │ Nutrition tips            [○ ] ││
│  └─────────────────────────────────┘│
│                                     │
│  INSIGHTS                           │
│  ┌─────────────────────────────────┐│
│  │ Weekly insights           [ ●] ││
│  │ Sundays at 10:00 AM             ││
│  └─────────────────────────────────┘│
│                                     │
│  QUIET HOURS                        │
│  ┌─────────────────────────────────┐│
│  │ Quiet hours               [ ●] ││
│  │ No reminders during these hours ││
│  ├─────────────────────────────────┤│
│  │ From                 10:00 PM › ││
│  │ To                    7:00 AM › ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ Occasional check-ins      [ ●] ││
│  │ If you've been away for a few   ││
│  │ days                            ││
│  └─────────────────────────────────┘│
│                                     │
│  Reminders are scheduled on your    │   ← bodySmall, onSurfaceVariant
│  device. Nothing is sent to a       │
│  server.                            │
└─────────────────────────────────────┘
```

**Components:**

- Rows: `ListItem` with `headlineContent` / `supportingContent` / `trailingContent = { Switch(...) }`. Not custom rows — the app already has `SwitchRow` in `ProfileScreen.kt`; reuse it and add a `supportingText` param rather than forking.
- Time picker: `TimePickerDialog` wrapping M3 `TimePicker`. Note M3 does not ship a `TimePickerDialog` composable — wrap `TimePicker` in an `AlertDialog` (known gap; don't hunt for the missing API).
- Day chips: `FilterChip` in a `FlowRow`. Seven chips: M T W T F S S. Default all selected.
- Time rows only render when the parent toggle is on — `AnimatedVisibility` so the section doesn't jump.
- Settings write immediately (no Save button) → `ReminderScheduler.rescheduleAll()` on each write, debounced ~300ms so dragging a time picker doesn't enqueue twenty work requests.

**Accessibility:** every `Switch` needs the row as its click target with a merged semantics node, or TalkBack reads the label and the switch as two unrelated things. `Modifier.toggleable(role = Role.Switch)` on the `ListItem`, and `Switch(onCheckedChange = null)`.

### 3.4 Backgrounded / reopened / long-absent behavior

| App state | Behavior |
|---|---|
| Foregrounded, reminder due | **Suppress.** The user is already in the app. Posting a "log your day" notification over the Log screen is absurd. `ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(RESUMED)` → skip and reschedule. |
| Backgrounded | Normal post. |
| Killed by user / OEM | WorkManager persists; chain resumes. Reboot receiver + 24h self-heal worker cover the gap. |
| Not opened 3+ days | `REENGAGEMENT` becomes eligible (rules 8 in [§2.6](#27-quiet-hours-suppression-and-not-being-annoying)). |
| Not opened 30+ days | Cancel all reminder chains. Someone who hasn't opened the app in a month does not want a daily 9 PM ping. Re-arm on next open. This is a courtesy that materially reduces uninstalls. |
| Signed out | `cancelAll()`. |
| Account deleted | `cancelAll()` — must be added to the existing delete flow alongside `database.clearAllTables()`. |

---

## 4. Android notification copy examples

Voice: calm, second-person, never urgent, never shaming, never medicalized. No exclamation marks in body copy. No streak-anxiety ("Don't lose your 12-day streak!"). No health claims.

Titles ≤ 40 chars (Android truncates ~40–50 in collapsed form). Bodies ≤ 100 chars for one collapsed line; anything longer needs `BigTextStyle`.

---

**1. Daily tracking reminder**

- **Title:** `How was today?`
- **Body:** `Take a moment to log how you're feeling.`
- **Trigger:** User-selected time (default 9:00 PM), on selected days.
- **Channel:** `tracking_reminders`
- **Deep link:** `genesyx://log`
- **Suppression:** Already logged today (`DailyLogDao`); quiet hours; app foregrounded; daily cap reached.

---

**2. Hydration reminder**

- **Title:** `Water break`
- **Body:** `A glass of water is a small win. Worth taking.`
- **Trigger:** Up to 2×/day at fixed offsets inside the user's waking window (default 11:00 AM, 4:00 PM). Not user-time-configurable in v1.1 — one time picker per category is enough surface area; revisit if asked for.
- **Channel:** `nutrition_wellness`
- **Deep link:** `genesyx://track`
- **Suppression:** Quiet hours; daily cap; app foregrounded. **Never posts on the same day as a `NUTRITION` reminder** — one wellness nudge per day, maximum.

---

**3. Nutrition reminder**

- **Title:** `Eating for your {phase} phase`
- **Body:** `A few foods that tend to help right now.`
- **Trigger:** Weekly, on cycle-phase transition (`CycleEngine` already computes phase). Fires at 10:00 AM the day the phase changes.
- **Channel:** `nutrition_wellness`
- **Deep link:** `genesyx://nutrition`
- **Suppression:** No `cycle_settings` row → **suppress entirely** (no phase, no message). Quiet hours; daily cap; already sent a `HYDRATION` reminder today.
- **Note:** `{phase}` interpolation must be lowercase and non-clinical — "luteal", "follicular". If `Phase` is unknown, do not post a generic fallback. Post nothing.

---

**4. Missed log reminder**

- **Title:** `Yesterday's still open`
- **Body:** `You can fill in how you felt — it only takes a minute.`
- **Trigger:** 9:00 AM, if no `DailyLogEntity` exists for the previous day **and** the user has logged ≥ 3 of the last 14 days (i.e. they're an active user who slipped, not a lapsed one).
- **Channel:** `tracking_reminders`
- **Deep link:** `genesyx://log?date=2026-07-08`
- **Suppression:** Max **1 per 3 days** — never on consecutive mornings. Two consecutive misses means they're busy, not forgetful. Quiet hours; daily cap.
- **Copy note:** "still open," not "you missed." The first is a door; the second is a verdict.

---

**5. Weekly insights reminder**

- **Title:** `Your week, at a glance`
- **Body:** `New patterns from the days you logged.`
- **Trigger:** Sundays 10:00 AM (day + time configurable).
- **Channel:** `weekly_insights` (`IMPORTANCE_LOW`, silent)
- **Deep link:** `genesyx://insights`
- **Suppression:** **Fewer than 2 logs in the past 7 days → suppress.** There are no patterns in one data point, and promising insight you can't deliver is the fastest way to lose a user. Quiet hours; daily cap.

---

**6. Re-engagement (bonus — the one to get right)**

- **Title:** `Still here whenever you are`
- **Body:** `No pressure. Your data's exactly where you left it.`
- **Trigger:** `daysSinceLastOpen >= 3`.
- **Channel:** `reengagement` (`IMPORTANCE_LOW`, silent, no badge)
- **Deep link:** `genesyx://home`
- **Suppression:** Max 1 per 7 days. **Hard stop after 3 consecutive unacknowledged.** Never during quiet hours. Never if `daysSinceLastOpen > 30` (cancel instead).
- **Copy note:** explicitly de-shames. Users returning to a health-tracking app after a lapse usually feel bad about the lapse. Copy that says "you've been away!" confirms the guilt and gets the app deleted.

---

## 5. Android Learn section information architecture

### 5.1 Where content lives — and why it's not in Supabase

**Decision: bundled in-app, as a Kotlin content object in `domain/content/`.**

This matches the existing, working pattern. `CycleContent.kt`, `NutritionContent.kt`, and `QuizContent.kt` are all in-code content objects. `LearnContent.kt` joins them.

Why not a Supabase `articles` table, which is the reflexive answer:

- **We have a direct, expensive precedent.** `PhRepository.refresh()` shipped querying a `ph_readings` table that didn't exist, and logged a non-fatal `E Ph` error after every single sign-in. It cost real debugging time and polluted every logcat. Shipping a Learn UI against an un-deployed `articles` table repeats that exactly.
- **The content doesn't change.** Ten launch articles, edited maybe quarterly. That is an app-release cadence, not a CMS cadence.
- **The architecture is local-first.** `ARCHITECTURE.md`: Room is the source of truth. Static reference content isn't user data — it doesn't belong in a synced table at all. Room holds *user* rows scoped by `userId`.
- **Offline is free.** Articles work on a plane, in a tunnel, on day one, with zero loading states on the critical path.
- **It sidesteps a Play review question.** No new network reads, no new Data Safety disclosure.

Cost of the decision: content edits ship with the app. Accept it. If v1.2 needs faster content velocity, add a Supabase `articles` table **behind a read-through cache with the bundled set as the offline fallback** — the bundled content becomes the seed, not dead weight.

**Bookmarks are different.** They are user-generated, must survive reinstall-with-same-account eventually, and must be deleted on account deletion. Put them in **Room** (`article_bookmarks`, `userId` + `articleId`), not DataStore. `AuthRepository.kt:58` already calls `database.clearAllTables()` on delete — bookmarks get wiped for free, with zero new deletion code and zero new GDPR surface. A DataStore `stringSet` would silently survive account deletion. That is a compliance bug waiting to be found.

This means **Room `version = 3` → `4`**, one new entity, one migration in `Migrations.kt`, and a schema JSON regenerated in `app/schemas/`. `exportSchema = true` is already on and `MigrationTestHelper` is already a dependency — write the migration test.

### 5.2 Navigation placement

**Learn is a bottom-nav tab. The bar carries six.**

> Revised twice during implementation. The brief first argued Learn should stay off the bar (Material
> 3 caps `NavigationBar` at 3–5). It then swapped Learn in for Profile, on the grounds that Profile is
> already reachable from the avatar menu on Home (`HomeScreen.kt:151`, `:171`). The owner wanted
> Profile visible in the bar regardless, so the final shape is **six tabs**, one past the Material 3
> maximum — a deliberate product call, not an oversight.

`Screen.bottomTabs` = Home / Track / Nutrition / Insights / **Learn** / Profile.

Six items leave ~60dp each at 360dp, which wrapped "Nutrition" onto a second line at the default
`labelSmall`. `GenesyxBottomNav` now pins labels to `fontSize = 9.sp`, `maxLines = 1`,
`softWrap = false`. **Any future label change must be checked at 360dp** — there is no headroom left.
A seventh tab is not possible without dropping labels entirely.

Learn also keeps its other entry points, which remain the natural in-context doors:

1. **Nutrition → "Learn more" section.** This replaces the existing dead-end. `NutritionScreen.kt:154-173` currently opens all three `Article` tiles into one identical hardcoded paragraph. Those tiles become real navigation into `learn/article/{slug}`, plus a "See all" row → `learn`. **This is the highest-leverage change in the entire brief** — it converts a visible bug into the feature's front door.
2. **Home → a single "Learn" card**, below the cycle card, surfacing the featured article.
3. **Profile → "Saved articles"** row, for bookmarks.

The Nutrition "Learn more" section sits **outside** the `if (state.cycleSetUp)` gate, so a user who
hasn't set up a cycle still gets an entry point. Educational content is most valuable to exactly that
user.

Deleting `NutritionContent.Article` and its three stub entries is required cleanup — `LearnContent.Article` supersedes it. That's an orphan created by this change, so it's in scope to remove.

```
Screen.kt additions:

  data object Learn         : Screen("learn")
  data object LearnCategory : Screen("learn/category/{category}") { fun create(c: String) = ... }
  data object LearnSearch   : Screen("learn/search")
  data object ArticleDetail : Screen("learn/article/{slug}")      { fun create(s: String) = ... }
  data object SavedArticles : Screen("learn/saved")
```

**Bottom-nav visibility:** `Learn`, `LearnCategory`, and `SavedArticles` **keep** the bottom bar (they're browsing surfaces — the user should be able to bail to Home). `ArticleDetail` and `LearnSearch` go into `noBottomNavRoutes` — reading and searching are immersive.

Watch out: `noBottomNavRoutes` is compared against `backStackEntry.destination.route`, which for a parameterized destination is the **pattern** (`"learn/article/{slug}"`), not the resolved path. Add `ArticleDetail.route` verbatim, exactly as `Invite.route` already is at `Screen.kt:48`.

---

## 6. Android screen-by-screen UX

### 6.1 Learn landing — `learn`

`LazyColumn`. Not `LazyVerticalGrid` — mixed item heights and a full-bleed hero make a grid fight you.

```
┌─────────────────────────────────────┐
│  Learn                        🔖 🔍 │  ← LargeTopAppBar, collapses on scroll
├─────────────────────────────────────┤
│  ┌─────────────────────────────────┐│
│  │                                 ││
│  │   [hero image, 16:9]            ││  ← Featured. Single item, full-bleed.
│  │                                 ││
│  │   GETTING STARTED               ││    Eyebrow (existing component)
│  │   Your first week with Genesyx  ││    headlineSmall
│  │   5 min read                    ││    labelMedium, onSurfaceVariant
│  └─────────────────────────────────┘│
│                                     │
│  [ All ][Tracking][Nutrition]…      │  ← LazyRow of FilterChip, horizontal scroll
│                                     │
│  ┌───┬─────────────────────────────┐│
│  │▧  │ Why logging beats           ││  ← ListItem, 72dp thumb, 2-line title max
│  │   │ remembering                 ││
│  │   │ Tracking · 4 min            ││
│  ├───┼─────────────────────────────┤│
│  │▧  │ Hydration, without the      ││
│  │   │ 8-glass myth                ││
│  │   │ Nutrition · 3 min           ││
│  └───┴─────────────────────────────┘│
└─────────────────────────────────────┘
```

- Filter chips filter **in place** on the landing screen (no navigation) for the common case. `LearnCategory` as a distinct route exists only to support deep links from CTAs.
- Bookmark icon in the app bar → `SavedArticles`. Badge with a count if > 0.
- `key = { it.id }` on `items()`. Without it, filter-chip changes recompose every row and lose scroll position.

**First-time hint:** a single dismissible `Card` above the featured item on first visit — "Short reads on tracking, nutrition, and what your patterns mean." Dismissal persisted as `learn_intro_seen` in DataStore. Not a coach-mark overlay, not a tooltip tour. One card, one dismiss.

### 6.2 Article list — `learn/category/{category}`

Same `ListItem` rows, `TopAppBar` titled with the category. Trivially a filtered `LearnContent.articles`. No pagination — ten articles.

### 6.3 Article detail — `learn/article/{slug}`

```
┌─────────────────────────────────────┐
│  ←                          🔖  ⤴   │  ← TopAppBar, transparent over hero,
│                                     │    solid after scroll (enterAlwaysScrollBehavior)
│    [ hero image, 16:9 ]             │
│                                     │
│  TRACKING · 4 MIN READ              │  ← Eyebrow
│                                     │
│  Why logging beats remembering      │  ← headlineMedium
│                                     │
│  Memory is reconstructive…          │  ← bodyLarge, lineHeight 1.6×
│                                     │
│  ## What to notice                  │  ← titleLarge
│  …                                  │
│                                     │
│  ┌─────────────────────────────────┐│
│  │  Ready to try it?               ││  ← CTA module. Card, surfaceVariant.
│  │  Log today in under a minute.   ││
│  │  [ Open today's log ]           ││    → genesyx://log
│  └─────────────────────────────────┘│
│                                     │
│  ─────────────────────────────────  │
│  This is educational content and    │  ← disclaimer, bodySmall, only when
│  not medical advice. …              │    disclaimerRequired == true
│  ─────────────────────────────────  │
│                                     │
│  Related                            │
│  ┌───┬─────────────────────────────┐│
│  │▧  │ Reading your trends         ││
│  └───┴─────────────────────────────┘│
└─────────────────────────────────────┘
```

- **Hero images.** Every article ships one: `learn_hero_*.jpg` in `drawable-nodpi`, 1080px wide, 16:9, ~46–147 KB each (864 KB total; release APK was 4.7 MB). `Article.heroImage` is nullable and `ArticleHero` falls back to a category-tinted brand gradient, so a future article without art still lays out correctly. The current set is **AI-generated** (Higgsfield, Jul 2026) — calm editorial photography plus two abstract graphics, no people's faces, no text, no clinical objects. Replaceable by setting one field. Abstract heroes must survive being shrunk to a 64dp thumbnail; the first pair failed this and were regenerated with stronger contrast.
- **Body rendering.** Do **not** add a Markdown dependency for ten articles. Model the body as `List<ArticleBlock>` — a sealed interface (`Paragraph`, `Heading`, `BulletList`, `Callout`, `Quote`). Renders in a `Column` with an exhaustive `when`. Type-safe, zero deps, previewable, and it forces content into a consistent shape. If v1.2 moves content server-side, serialize the same sealed hierarchy as JSON.
- **Reading time.** Precomputed and stored per article, not derived at runtime. `readingTime` is editorial ("4 min"), not `wordCount / 200` — a bulleted checklist reads faster than dense prose and the arithmetic will lie.
- **Share** → `Intent.ACTION_SEND`, `text/plain`, `"{title} — https://genesyx.co.uk/blog/{slug}"`, wrapped in `Intent.createChooser`. **Requires the marketing site to host `/blog/{slug}`.** If those URLs don't exist, share the Play Store link instead. Do not ship a share button that produces a 404 — decide before build.
- **Bookmark** → optimistic toggle, Room write, `Snackbar` "Saved" with an "Undo" action.
- **Scroll position** survives config change via `rememberLazyListState()` + `rememberSaveable`.

### 6.4 Search — `learn/search`

`SearchBar` (M3). Client-side, case-insensitive substring over `title + excerpt + tags`. Ten articles: no index, no debounce needed, no `Fuse`-style fuzzy matching. Filter on every keystroke; it's ten string comparisons.

Empty query → show category chips and a "Popular" list. Empty result → [§10](#10-edge-cases--loading--empty--error-states).

### 6.5 Saved articles — `learn/saved`

`LazyColumn` over the Room-backed bookmark join. Swipe-to-remove (`SwipeToDismissBox`) with undo `Snackbar`. Empty state per [§10](#10-edge-cases--loading--empty--error-states).

---

## 7. Ten launch articles

Category enum: `TRACKING`, `NUTRITION`, `INSIGHTS`, `WELLNESS`, `GETTING_STARTED`, `PH_WELLNESS`.

All copy below is **direction, not final text.** It needs an editorial pass and — for anything touching health — review by someone qualified. See [§8](#8-ph-related-article-cluster).

---

**1. Your first week with Genesyx** · `getting-started-first-week` · **FEATURED**
- **Excerpt:** What to do on day one, day three, and day seven — and what to ignore until later.
- **Category:** `GETTING_STARTED` · **Tags:** onboarding, basics · **Reading time:** 5 min
- **Hero:** Soft-focus morning light on a bedside table, phone face-down. Calm, not clinical. Brand lavender in the shadows.
- **Outline:** Why the first week is about consistency, not completeness → Day 1: set your cycle, log once → Days 2–3: notice how little time it takes → Day 7: your first patterns appear (and why they're still noisy) → What to ignore for now.
- **CTA:** `Open today's log` → `genesyx://log`

**2. Why logging beats remembering** · `why-logging-beats-remembering`
- **Excerpt:** Memory rewrites the past to fit the present. A log doesn't.
- **Category:** `TRACKING` · **Tags:** tracking, habits, basics · **Reading time:** 4 min
- **Hero:** Overhead, open notebook with a single line written. Warm neutral.
- **Outline:** How recall bias works, briefly and without jargon → "I felt terrible all week" vs. what you logged → Why small, boring entries beat detailed ones you skip → What's actually worth logging.
- **CTA:** `Log how you feel today` → `genesyx://log`

**3. Symptoms and meals: what's worth writing down** · `what-to-log`
- **Excerpt:** You don't need to log everything. Here's the short list.
- **Category:** `TRACKING` · **Tags:** tracking, symptoms, nutrition · **Reading time:** 4 min
- **Hero:** Flat-lay, three simple objects — a glass of water, a pen, a piece of fruit.
- **Outline:** The completeness trap → Four things worth logging daily → Two worth logging weekly → Why "nothing to report" is a real, useful entry → Consistency > detail.
- **CTA:** `Open today's log` → `genesyx://log`

**4. Hydration, without the eight-glass myth** · `hydration-basics`
- **Excerpt:** Where "eight glasses a day" came from, and what to do instead.
- **Category:** `NUTRITION` · **Tags:** hydration, nutrition, myths · **Reading time:** 3 min
- **Hero:** Water being poured, close, side-lit. Movement.
- **Outline:** The 1945 misreading behind the rule → Thirst is a decent signal for most healthy adults → Food is a real water source → Practical anchors (a glass with each meal) → *Brief:* when persistent thirst is worth mentioning to a clinician.
- **CTA:** `Set a hydration reminder` → `genesyx://reminder_settings`
- **`disclaimerRequired`: true**

**5. Eating with your cycle, not against it** · `eating-with-your-cycle`
- **Excerpt:** What changes across the four phases — and what genuinely doesn't.
- **Category:** `NUTRITION` · **Tags:** nutrition, cycle, phases · **Reading time:** 6 min
- **Hero:** Four small plates in a soft grid, each in one phase accent (the four `PhaseFood` accent colors from `NutritionContent.kt`).
- **Outline:** What actually shifts (energy, appetite, iron needs during menstruation) → What's overstated → Phase by phase, one practical idea each → **Explicitly:** no food changes your cycle, and none determines anything about a future pregnancy.
- **CTA:** `See this phase's foods` → `genesyx://nutrition`
- **`disclaimerRequired`: true`**
- **Editorial guardrail:** this is the article most likely to drift into overclaiming. It must not imply diet influences conception outcomes or fetal sex. See [§8.0](#80-the-hard-editorial-line).

**6. A gentle guide to supplements** · `gentle-guide-supplements`
- **Excerpt:** What the evidence supports, what it doesn't, and why you should talk to someone before starting.
- **Category:** `NUTRITION` · **Tags:** supplements, nutrition · **Reading time:** 6 min
- **Hero:** Neutral, minimal — an open palm, out of focus. **No pills, no bottles, no packaging.** Do not make this look like an ad.
- **Outline:** Supplements are supplementary → Folate/folic acid is the one with genuinely strong, mainstream guidance for anyone who might become pregnant → Everything else: "ask, don't assume" → Interactions are real → How to raise it with a GP or pharmacist.
- **CTA:** `Read about talking to your GP` → `genesyx://learn/article/when-to-seek-support`
- **`disclaimerRequired`: true`**
- **Editorial guardrail:** name **no** doses. Name no brands. Genesyx sells supplements (`mysupplementfactory.com`) — this article must be scrupulously non-promotional or it is both an ethics problem and a Play policy problem.

**7. What "insights" actually means** · `what-insights-mean`
- **Excerpt:** Your app can spot correlations. It cannot spot causes. That distinction is the whole game.
- **Category:** `INSIGHTS` · **Tags:** insights, data, basics · **Reading time:** 5 min
- **Hero:** Two simple line traces, loosely parallel, abstract. No axes, no numbers.
- **Outline:** What Genesyx compares and how → Correlation vs. causation, made concrete with one example → Why three weeks of data says almost nothing and three months says something → How to hold an insight lightly.
- **CTA:** `See your insights` → `genesyx://insights`

**8. Reading your trends without over-reading them** · `reading-your-trends`
- **Excerpt:** One bad day is noise. Six weeks is a signal. Here's how to tell.
- **Category:** `INSIGHTS` · **Tags:** insights, trends, patterns · **Reading time:** 5 min
- **Hero:** A scatter of dots resolving into a faint trend line.
- **Outline:** Variance is normal and expected → Why a single outlier means nothing → What a real trend looks like → When a trend is worth raising with a clinician (persistent, and new for you) → The trap of tracking harder when you feel worse.
- **CTA:** `See your insights` → `genesyx://insights`
- **`disclaimerRequired`: true`**

**9. Small habits that hold** · `small-habits-that-hold`
- **Excerpt:** The habit research says: make it smaller than feels worthwhile.
- **Category:** `WELLNESS` · **Tags:** habits, wellness, consistency · **Reading time:** 4 min
- **Hero:** A worn path across grass — a desire line. Quiet, human.
- **Outline:** Why ambitious habits fail in week two → Anchoring to something that already happens → The two-minute version of everything → Missing a day is fine; missing two is the pattern to watch → Why streaks help some and harm others.
- **CTA:** `Set a daily reminder` → `genesyx://reminder_settings`

**10. Using what you learn** · `using-what-you-learn`
- **Excerpt:** Data is only useful if it changes something. Here's how to close the loop.
- **Category:** `INSIGHTS` · **Tags:** insights, decisions, wellness · **Reading time:** 4 min
- **Hero:** A hand adjusting a dial, warm light. Agency.
- **Outline:** Notice → hypothesize → change one thing → wait a full cycle → check → Why changing three things at once teaches you nothing → When to stop experimenting and talk to a professional.
- **CTA:** `See your insights` → `genesyx://insights`
- **`disclaimerRequired`: true`**

---

## 8. pH-related article cluster

### 8.0 The hard editorial line

**Read this before writing a word of pH content.**

An earlier build shipped a "Did you know?" modal in the onboarding quiz claiming that "even pH balance can subtly influence the likelihood of conceiving a boy or girl." It was caught as a **launch blocker** and removed (`domain/content/QuizContent.kt`), and the gender question was rewritten. A repo-wide grep for `boy or girl`, `sex-selection`, and `sway` now returns zero user-visible hits, and it must stay that way.

That claim was removed because it is **not supported by evidence**, and because sex-selection framing in a fertility app is a serious ethical and regulatory liability. Reintroducing it inside a Learn article — softened, hedged, "just educational" — would be the same mistake wearing a better outfit.

**Absolute prohibitions for this cluster.** No article may:

- connect pH to conception odds, fertility outcomes, or fetal sex, in any direction, at any hedge level;
- describe pH as something to "optimize," "correct," "balance," or "restore";
- recommend douching, alkaline diets, alkaline water, or any pH-modifying product or practice;
- present a pH value as normal/abnormal, healthy/unhealthy, good/bad;
- imply a reading is diagnostic of infection, or that a reading substitutes for testing.

**Required of every article in this cluster:**

- `disclaimerRequired = true` (non-negotiable, enforce in a test);
- an explicit "when to seek professional support" section, not just a footer line;
- language framing pH as **an observation the user records**, never a target they pursue.

**Add a CI grep** over `LearnContent.kt` and `QuizContent.kt` for the banned phrases, failing the build on a hit. The last time this was caught, it was caught by a human on a device the night before release. Automate it.

**Every article in this cluster must be reviewed by a qualified clinician before release.** Nothing here is a substitute for that review, and the drafter of this brief is not qualified to provide it. If clinical review can't happen before v1.1, **ship the Learn section without this cluster.** The other ten articles stand alone. Nothing about Learn requires pH content to exist.

The `PH_WELLNESS` category should not appear as a filter chip if the cluster is empty.

### 8.1 Standard disclaimer

Rendered above the Related section whenever `disclaimerRequired == true`. Single source of truth in `strings.xml` as `learn_medical_disclaimer`.

> **This is educational content, not medical advice.** It can't account for your individual circumstances, and it isn't a substitute for talking to a doctor, nurse, or pharmacist. If something feels wrong, or you're worried, please speak to a healthcare professional.

### 8.2 The cluster (four articles)

**P1. What pH means — and what it doesn't** · `what-ph-means`
- **Excerpt:** A short, honest explanation of a number that gets talked about a lot and understood rarely.
- **Category:** `PH_WELLNESS` · **Tags:** ph, basics · **Reading time:** 4 min
- **Hero:** Abstract gradient, cool to warm. **No test strips, no color charts, no clinical imagery.**
- **Outline:** pH is a measure of acidity, on a scale — that's all it is → It varies, in everyone, across a day and across a cycle → **There is no single "right" number, and this app will never tell you your reading is good or bad** → Why Genesyx records it: because *your own* variation over time is more meaningful to you and your clinician than any single value → What it cannot tell you (the explicit list) → When to seek support.
- **CTA:** `See your pH history` → `genesyx://insights`

**P2. Why your body regulates on its own** · `body-self-regulation`
- **Excerpt:** The short answer to "how do I fix my pH" is: you almost certainly don't need to.
- **Category:** `PH_WELLNESS` · **Tags:** ph, wellness · **Reading time:** 4 min
- **Hero:** Still water, gentle surface movement.
- **Outline:** Healthy bodies regulate tightly, without help → **Douching and pH-altering products can disrupt this and are widely advised against** → Products marketed for "balance" are largely unnecessary and sometimes harmful → What actually supports general wellbeing (sleep, food, hydration, not smoking) — framed as general health, not pH intervention → When a change *is* worth a conversation with a professional.
- **CTA:** `Read: when to seek support` → `genesyx://learn/article/when-to-seek-support`
- **Editorial note:** the anti-douching guidance is the single most useful, most evidence-backed, and most protective thing in the whole cluster. Lead with it. It is also the clearest signal that this app is not selling anything here.

**P3. Hydration, food, and general wellbeing** · `ph-nutrition-hydration`
- **Excerpt:** What everyday habits genuinely support — and the claims that don't hold up.
- **Category:** `PH_WELLNESS` · **Tags:** ph, nutrition, hydration · **Reading time:** 5 min
- **Hero:** Simple whole foods, soft daylight.
- **Outline:** **"Alkaline diets" and "alkaline water" do not meaningfully change your body's pH** — say this plainly and early → Blood pH is tightly regulated and does not respond to diet in healthy people → What good hydration and varied food *do* support, honestly and modestly → Why "supports wellness" is a claim worth being suspicious of, including when we make it → When to seek support.
- **CTA:** `See this phase's foods` → `genesyx://nutrition`
- **Editorial note:** this article exists primarily to *debunk*. If it reads as endorsement of alkaline anything, it has failed and must be rewritten.

**P4. When to talk to a professional** · `when-to-seek-support`
- **Excerpt:** The signs that are worth a call, and how to have the conversation.
- **Category:** `PH_WELLNESS` · **Tags:** ph, support, wellbeing · **Reading time:** 4 min
- **Hero:** Two chairs, a window. Nobody in frame.
- **Outline:** This app cannot diagnose anything, and neither can a number → Symptoms that warrant contacting a clinician (unusual discharge, odor, itching, burning, pain, anything new and persistent) — **stated plainly, without hedging, without euphemism** → How to bring your logs to an appointment usefully → **You never need a reason or a symptom to ask a question about your body.**
- **CTA:** `Open today's log` → `genesyx://log`
- **Editorial note:** the symptom list must be reviewed by a clinician. Under-listing here is a safety failure, not a copy problem. This is the most important article in the cluster; write it first and let the others defer to it.

---

## 9. Android data models

### 9.1 Notification settings

DataStore-backed, in `GenesyxPreferencesDataStore` alongside the existing keys. `push_enabled` (already at line 39) is reused as the master switch.

`LocalTime` and `Set<DayOfWeek>` don't have `Preferences` key types — persist as `Int` (minutes from midnight) and a `stringSetPreferencesKey` of `DayOfWeek.name`. Map at the repository boundary so the domain model stays clean.

```kotlin
package com.genesyx.app.notifications.model

import java.time.DayOfWeek
import java.time.LocalTime

/** Everything the reminder scheduler needs. Immutable; a write triggers rescheduleAll(). */
data class NotificationSettings(
    // Master switch — reuses the existing `push_enabled` DataStore key.
    val remindersEnabled: Boolean = false,

    // Per-category
    val trackingRemindersEnabled: Boolean = false,
    val missedLogRemindersEnabled: Boolean = false,
    val nutritionRemindersEnabled: Boolean = false,
    val hydrationRemindersEnabled: Boolean = false,
    val weeklyInsightsEnabled: Boolean = false,
    val reengagementEnabled: Boolean = true,   // opt-out, not opt-in; LOW importance + silent

    // Schedule
    val reminderTime: LocalTime = LocalTime.of(21, 0),
    val selectedDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val weeklyInsightsDay: DayOfWeek = DayOfWeek.SUNDAY,
    val weeklyInsightsTime: LocalTime = LocalTime.of(10, 0),

    // Quiet hours. May wrap midnight — quietHoursStart > quietHoursEnd is legal and expected.
    val quietHoursEnabled: Boolean = true,
    val quietHoursStart: LocalTime = LocalTime.of(22, 0),
    val quietHoursEnd: LocalTime = LocalTime.of(7, 0),

    // Permission state
    val pushPermissionStatus: PushPermissionStatus = PushPermissionStatus.NOT_ASKED,
    /** Set when the system dialog is first launched. Disambiguates NOT_ASKED from DENIED_PERMANENT. */
    val lastPromptedAt: Long? = null,

    // Suppression bookkeeping (see §2.7)
    val notificationsPostedToday: Int = 0,
    val postedTodayDate: String? = null,          // ISO date; reset counter when it rolls over
    val lastReengagementAt: Long? = null,
    val consecutiveIgnoredReengagements: Int = 0,
    val lastOpenedAt: Long? = null,
    /** Bumped when scheduling logic changes, forcing a rescheduleAll() on upgrade. */
    val scheduleVersion: Int = 1,
)
```

**On the brief's suggested fields.** Two were requested and are deliberately omitted:

- **`notificationChannelPreference`** — omitted. Channel settings (sound, vibration, importance) are owned by the OS on API 26+; an in-app mirror can only ever disagree with reality. Deep-link to system settings instead. Storing a preference the OS ignores is a bug you'll debug twice.
- **`deepLinkTarget`** — omitted from *settings*. A deep-link target is a property of a **notification**, not of a user's preferences. It belongs on `ReminderKind`, where it's exhaustive and compile-checked:

```kotlin
enum class ReminderKind(val channelId: String, val deepLink: String, val workName: String) {
    TRACKING(NotificationChannels.TRACKING, "genesyx://log", "reminder-tracking"),
    MISSED_LOG(NotificationChannels.TRACKING, "genesyx://log", "reminder-missed-log"),
    HYDRATION(NotificationChannels.NUTRITION, "genesyx://track", "reminder-hydration"),
    NUTRITION(NotificationChannels.NUTRITION, "genesyx://nutrition", "reminder-nutrition"),
    WEEKLY_INSIGHTS(NotificationChannels.INSIGHTS, "genesyx://insights", "reminder-insights"),
    REENGAGEMENT(NotificationChannels.REENGAGEMENT, "genesyx://home", "reminder-reengagement"),
}
```

### 9.2 Article

Bundled content — a plain domain model in `domain/content/`, no Room entity, no DTO. Only `isBookmarked` is user state, and it does **not** live on the model.

```kotlin
package com.genesyx.app.domain.content

enum class ArticleCategory(val label: String) {
    GETTING_STARTED("Getting started"),
    TRACKING("Tracking"),
    NUTRITION("Nutrition"),
    INSIGHTS("Insights"),
    WELLNESS("Wellness"),
    PH_WELLNESS("pH & wellbeing"),
}

/** Structured body. Beats a Markdown dependency for 14 bundled articles, and stays serializable. */
sealed interface ArticleBlock {
    data class Heading(val text: String) : ArticleBlock
    data class Paragraph(val text: String) : ArticleBlock
    data class BulletList(val items: List<String>) : ArticleBlock
    data class Callout(val text: String) : ArticleBlock
    data class Quote(val text: String, val attribution: String? = null) : ArticleBlock
}

enum class CtaType { OPEN_LOG, OPEN_TRACK, OPEN_NUTRITION, OPEN_INSIGHTS, OPEN_REMINDERS, OPEN_ARTICLE }

data class ArticleCta(val type: CtaType, val label: String, val target: String)

data class Article(
    val id: String,
    val slug: String,                       // stable; used in deep links and share URLs — never change one
    val title: String,
    val excerpt: String,
    val body: List<ArticleBlock>,
    val category: ArticleCategory,
    val tags: List<String>,
    val author: String = "The Genesyx team",
    val publishDate: LocalDate,
    val readingTime: String,                // editorial, e.g. "4 min read" — not computed
    @DrawableRes val heroImage: Int,
    val featured: Boolean = false,
    val relatedArticleIds: List<String> = emptyList(),
    val cta: ArticleCta? = null,
    /** True → render the medical disclaimer. Enforced true for every PH_WELLNESS article by test. */
    val disclaimerRequired: Boolean = false,
)
```

**`isBookmarked` is not a field on `Article`.** The brief asks for it; putting it there would make a `val`-only bundled constant carry mutable per-user state, and force every screen to rebuild the content list to toggle a star. Instead the ViewModel combines the static list with a Room `Flow`:

```kotlin
data class ArticleListItem(val article: Article, val isBookmarked: Boolean)

val items: StateFlow<List<ArticleListItem>> =
    bookmarkDao.observeIds(userId)
        .map { saved -> LearnContent.articles.map { ArticleListItem(it, it.id in saved) } }
        .stateIn(...)
```

`relatedArticleIds` over `relatedArticles: List<Article>` for the same reason: a self-referencing `data class` graph can't be built as a `val` list without lateinit or lazy backdoors. Resolve IDs at the ViewModel.

### 9.3 Bookmarks — Room

`GenesyxDatabase` `version = 3` → `4`. Migration in `Migrations.kt`, schema JSON regenerated, `MigrationTestHelper` test added (both are already set up).

```kotlin
@Entity(
    tableName = "article_bookmarks",
    primaryKeys = ["userId", "articleId"],
    indices = [Index("userId")],
)
data class ArticleBookmarkEntity(
    val userId: String,      // scoped like every other entity — see GenesyxDatabase KDoc
    val articleId: String,
    val savedAt: Long,
)
```

`userId`-scoped, matching every other entity in the DB. Wiped automatically by the existing `database.clearAllTables()` on account deletion (`AuthRepository.kt:58`) — **no new deletion code, and no new row in the Data Safety form.** That is the whole reason this is in Room and not DataStore.

Not synced to Supabase in v1.1. A bookmark is worth less than the schema change, the RLS policy, and the sync-conflict surface it would cost.

---

## 10. Edge cases, loading, empty, and error states

### 10.1 Learn

| State | When | Treatment |
|---|---|---|
| Loading | Essentially never — content is a compile-time constant | **No skeleton, no spinner.** Bundled content renders on first frame. Adding a fake shimmer here would be a lie about latency. |
| Hero image loading | Drawable from resources | Instant. No placeholder needed. |
| Empty search | Query matches nothing | Icon + "No articles match "{query}"" + "Clear search" text button + the category chips below, so there's a way forward. |
| Empty category | Only reachable if `PH_WELLNESS` ships empty | Don't render the chip at all. An empty category the user can tap into is a bug, not a state. |
| Empty bookmarks | No saved articles | Bookmark outline icon + "Nothing saved yet" + "Tap the bookmark icon on any article to keep it here." + "Browse articles" button → `learn`. |
| Article not found | Bad deep link (`genesyx://learn/article/typo`) | `popBackStack()` to `learn` + `Snackbar` "That article isn't available." **Never a blank screen, never a crash.** Test with `adb shell am start -a android.intent.action.VIEW -d "genesyx://learn/article/nope"`. |
| Error | There is no network call | **No error state exists.** Do not build one. |

### 10.2 Notifications

| State | When | Treatment |
|---|---|---|
| Permission denied permanently | Two system-dialog denials | Section dims, "Open settings" replaces "Enable". Never re-launch the system dialog — it silently no-ops and the user thinks the app is broken. |
| Notifications blocked in system settings | `areNotificationsEnabled() == false` | Banner + "Open settings". Re-check on `onResume`. |
| Notifications enabled, channel disabled | User killed one channel in system settings | `NotificationManagerCompat.getNotificationChannel(id).importance == IMPORTANCE_NONE` → show that row's toggle as off with "Turned off in phone settings". This is subtle, common, and almost never handled. |
| Reminder due while app foregrounded | `ProcessLifecycleOwner` state ≥ RESUMED | Suppress, reschedule. No in-app snackbar substitute — it's an interruption either way. |
| Reminder due while signed out | Session cleared | Suppress + `cancelAll()`. Must never deep-link past the auth gate added in `49d07f2`. |
| Reminder due, action already done | Logged today | Suppress. Query `DailyLogDao` at post time, not schedule time. |
| Quiet hours spanning midnight | `start=22:00`, `end=07:00` | `if (start > end) now >= start \|\| now < end else now in start..<end`. **Write the unit test first.** This one line will be wrong on the first attempt. |
| Timezone / DST change | User flies, or clocks change | `nextOccurrence` recomputes from local wall-clock each run → self-corrects in one cycle. Register `ACTION_TIMEZONE_CHANGED`. |
| Device rebooted | — | WorkManager persists. Boot receiver is belt-and-braces for OEM killers. |
| WorkManager chain lost (OEM kill) | Xiaomi/Huawei/Samsung aggressive task management | 24h unique periodic self-heal worker re-enqueues missing chains. **Cannot be fully solved.** Don't pretend otherwise in the design. |
| 30+ days inactive | — | `cancelAll()`. Re-arm on next open. |
| Account deleted | Delete flow | `cancelAll()` — **add to the existing delete path.** A deleted account whose phone keeps buzzing at 9 PM is the worst possible outcome of this feature. |

---

## 11. Jetpack Compose developer handoff notes

### 11.1 Build order

Each step is independently shippable and independently verifiable. Do not start step 3 before step 2 passes on a device.

1. **Preflight.** Fix the stale `FeatureFlags.PH_TRACKING` KDoc ([§0](#0-preflight-what-the-repo-actually-looks-like)). Reconcile `CLAUDE.md` (versionCode 7, pH sync done). Pure docs; no behavior change. *Verify:* `grep -n "v1.1" PhRepository.kt` returns nothing and the KDoc no longer claims guards exist.
2. **Learn, read-only.** `LearnContent.kt` with 3 real articles, `Screen` routes, landing + detail, wire `NutritionScreen`'s three dead tiles to real navigation. Delete `NutritionContent.Article`. No bookmarks, no search, no Room change. *Verify:* the three Nutrition tiles open three **different** articles. That single check kills the existing bug.
3. **Learn, complete.** Remaining articles, search, categories, related, share, CTAs, first-time hint. Still no Room change. *Verify:* every CTA deep link resolves; `genesyx://learn/article/nope` doesn't crash.
4. **Bookmarks.** Room 3→4, entity, DAO, migration + `MigrationTestHelper` test, saved screen, Profile row. *Verify:* migration test green; bookmark survives process death; account deletion clears bookmarks.
5. **Notification plumbing.** Channels, permission state machine, settings screen, `POST_NOTIFICATIONS`. **No scheduling yet.** Flip `FeatureFlags.PUSH_NOTIFICATIONS = true`. *Verify:* the full permission matrix in [§3.2](#32-permission-outcome-states) on API 33+ **and** on an API 26–32 emulator (different code path — this is where the bugs are).
6. **Scheduling.** `ReminderScheduler`, `ReminderWorker`, `ReminderNotifier`, suppression rules, deep links, `singleTop` + `onNewIntent`. *Verify:* the device checklist below.
7. **Re-engagement + self-heal.** Boot receiver, 24h re-arm worker, inactivity cancellation. Last, because it's the least valuable and the most annoying if wrong.

The pH cluster ([§8](#8-ph-related-article-cluster)) gates on clinical review and can land any time after step 3, or not at all.

### 11.2 Conventions to match

Read these before writing anything; the codebase is consistent and it would be a shame to break that.

- **Content lives in `domain/content/`** as top-level `val`s. `CycleContent.kt`, `NutritionContent.kt`, `QuizContent.kt`. Follow them.
- **Schedulers are interfaces** with a `WorkManager*` impl, so repositories stay JVM-testable (`PhSyncScheduler.kt:16`).
- **Workers get deps via Hilt `EntryPoint`**, not `@HiltWorker` — WorkManager constructs them (`PhSyncWorker.kt:23-33`). Do not introduce `HiltWorkerFactory` for one worker; match the existing shape.
- **DataStore access goes through `GenesyxPreferencesDataStore`**, with a typed `Keys` object and `Flow` accessors. Not raw `dataStore.data` in a ViewModel.
- **Repositories return `DataResult<T>`** (`core/result/DataResult.kt`).
- **`Logger.d` is stripped outside DEV.** Don't rely on it for release diagnostics.
- **Existing components:** `ScreenHeader`, `Eyebrow`, `GxBackButton` (48dp — that was a real bug fix, don't shrink it), `SwitchRow`. Reuse, extend, don't fork.
- **Theme:** `ElectricLavender` is the accent (`ui/theme/Color.kt`). Both light and dark must work — the Home hero image already forced a theme-aware branch; don't regress it.
- **Min touch target 48dp.** The quiz back-arrow shipped at 44dp and was a filed bug.

### 11.3 State

Standard for this repo: `@HiltViewModel`, `StateFlow<UiState>`, `collectAsState()` in the composable, `hiltViewModel()` at the route.

```kotlin
@HiltViewModel
class LearnViewModel @Inject constructor(
    private val bookmarks: BookmarkRepository,
    private val session: SessionRepository,
) : ViewModel() {
    private val selectedCategory = MutableStateFlow<ArticleCategory?>(null)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<LearnUiState> = combine(
        selectedCategory, query, bookmarks.observeIds(),
    ) { category, q, saved -> LearnUiState(items = filter(category, q, saved), ...) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LearnUiState())
}
```

`SharingStarted.WhileSubscribed(5_000)` matches the existing ViewModels — check `HomeViewModel` and keep it identical.

`ArticleDetailViewModel` takes `slug` from `SavedStateHandle`. If the slug doesn't resolve, emit `ArticleUiState.NotFound` — do **not** throw. Bad deep links come from the outside world.

### 11.4 Testing

**Unit (JVM) — the part that actually catches bugs:**

- `quietHoursContains(now, start, end)` — **the midnight wrap.** Write this test before the implementation.
- `nextOccurrence(settings, now)` across: DST spring-forward, DST fall-back, timezone change, all-days-deselected (must return null, not loop), today-already-past-the-time.
- `ReminderNotifier.shouldPost(...)` — every rule in [§2.7](#27-quiet-hours-suppression-and-not-being-annoying), in order. Table-driven.
- `PushPermissionStatus` resolution, especially `NOT_ASKED` vs `DENIED_PERMANENT` disambiguation via `lastPromptedAt`.
- **Content invariants** (these are cheap and will save you):
  - every `slug` unique;
  - every `relatedArticleIds` entry resolves to a real article;
  - every `CtaType.OPEN_ARTICLE` target resolves;
  - **every `PH_WELLNESS` article has `disclaimerRequired == true`**;
  - **no banned phrase appears anywhere in `LearnContent`** — `boy or girl`, `sex-selection`, `sway`, `alkaline diet`, `douch`, `optimize your ph`, `balance your ph`. See [§8.0](#80-the-hard-editorial-line). This test is the guardrail; treat a failure as a release blocker, not a lint warning.

**Instrumented:**

- Room 3→4 migration via `MigrationTestHelper` (`app/schemas/` is already exported).
- Bookmark toggle → survives process death.
- Compose: article detail renders the disclaimer iff `disclaimerRequired`.
- `ArticleDetail` with an unknown slug → no crash.

**Manual, on-device (no substitute exists for these):**

- Cold start from a notification tap, app **not** running. *This is the one that breaks* — see [§2.6](#26-deep-linking-the-part-that-will-bite-you) trap (b).
- Notification tap while app is **foregrounded** → must not create a second `MainActivity`. Check `adb shell dumpsys activity activities | grep MainActivity` for a single instance.
- Deny permission twice → button must become "Open settings" and the system dialog must never be launched again.
- Turn notifications off in system settings while app is backgrounded → return to app → banner appears **without** needing to re-navigate.
- Reboot → reminder still fires.
- Airplane mode → reminder still fires. (If it doesn't, someone added a network constraint. Remove it.)
- Set reminder for 2 minutes out, background the app, wait. Then set one, log your day, and confirm it does **not** fire.
- Delete account → confirm no reminder ever fires again.

### 11.5 Things not to do

- **Don't add Firebase/FCM.** Nothing in this brief needs it. It adds a Data Safety disclosure, a Google Services plugin, a device token lifecycle, and a server. See [§1](#feature-1--reminders-not-push).
- **Don't add a Markdown library** for 14 bundled articles. `sealed interface ArticleBlock` is smaller, safer, and previewable.
- **Don't add an image loader (Coil/Glide).** Hero images are bundled drawables. `painterResource` is enough. If content ever moves server-side, add Coil then.
- **Don't use `PeriodicWorkRequest`.** 15-minute minimum, no guarantee of *when* in the window. A 9:00 PM reminder must arrive near 9:00 PM. See [§2.3](#23-scheduling-copy-the-phsyncscheduler-pattern).
- **Don't request `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`.** Play restricts them to alarm/calendar apps. A wellness reminder doesn't qualify and it's a plausible review rejection.
- **Don't add a seventh bottom-nav tab.** The bar already carries six (one past the Material 3 ceiling) and labels are pinned at 9sp to fit. There is no headroom. See [§5.2](#52-navigation-placement).
- **Don't add an image loader.** Article heroes are bundled `drawable-nodpi` JPEGs read with `painterResource`. Coil/Glide earn their keep only if content moves server-side.
- **Don't mirror OS channel settings in-app.** The OS owns sound/vibration/importance on API 26+. Deep-link to system settings.
- **Don't build a loading state for Learn.** The content is a compile-time constant. A shimmer would be theatre.
- **Don't write the pH cluster without clinical review.** [§8.0](#80-the-hard-editorial-line). Ship the other ten articles instead.

---

## Open questions for the owner

1. **Does `genesyx.co.uk/pages/blog/{slug}` exist, or will it?** The Share action needs a real destination. If not, share the Play Store listing URL instead — but decide before building [§6.3](#63-article-detail--learnarticleslug), not after.
2. **Who clinically reviews the pH cluster?** Without a named reviewer, [§8](#8-ph-related-article-cluster) does not ship. The other ten articles are unaffected.
3. **Does adding reminders change the Play Data Safety form?** Local notifications collect nothing and should not — worth a deliberate confirmation given the app is already through review.
4. **Supplements article ([§7](#7-ten-launch-articles) #6) and commercial conflict.** Genesyx is adjacent to a supplements business. Either the article is scrupulously non-promotional (my strong recommendation, and what's drafted) or it's cut. There is no acceptable middle.
5. **Reminder default time — 9:00 PM.** Assumed, not researched. Worth checking against whatever engagement data exists.
