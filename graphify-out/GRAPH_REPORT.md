# Graph Report - .  (2026-08-13)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 2367 nodes · 5091 edges · 174 communities (112 shown, 62 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 121 edges (avg confidence: 0.78)
- Token cost: 104,142 input · 5,675 output

## Graph Freshness
- Built from commit: `7ffab0f0`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Supplement Remote Data Sources
- Symptom & Hydration Insights
- Auth Service
- Repo Docs & Brand Assets
- Reminder Policy Core
- Profile ViewModel
- Notification Permission Logic
- Onboarding Quiz UI
- pH Repository
- Streak Engine Tests
- Daily Log Repository Tests
- Client Repository & Results
- Preferences DataStore
- Meal Log UI & Data
- App Config & DI Core
- User Supplement DAO
- Reminder Policy Tests
- Nutrition Screen UI
- Daily Log Domain Models
- Insights Screen UI
- UI/DB Spec Inventory
- Partner Repository
- Cycle Engine Models
- Preferences Repository
- Navigation Screens
- Supplement Insight Logic
- Home Screen UI
- Room Type Converters
- Notification Settings Repository
- App Navigation Start State
- Clients & Log Screens
- Cycle Content & Phase Copy
- Streak Engine & DataStore
- Auth Screen UI
- pH Reading DAO
- Profile Remote Data Source
- Auth Repository
- Brand UI & pH Status
- User Supplement Repository Tests
- pH Copy & Citations
- Consistency Insight Logic
- Profile Screen UI
- Reminder Scheduler
- Partner Invite DAO
- Cycle Regularity Logic Tests
- Legacy File Path Refs
- Profile DAO & Repository
- Learn Content Tests
- Sleep Detail Screen
- Meal Log Migration & DB Module
- Supplement Insight Logic Tests
- Cycle Engine Tests
- Auth Repository Tests
- Cloud API & Analytics DI
- Daily Log Remote Data
- Reminder Settings ViewModel
- Track Calendar UI
- Day Markers Tests
- Cycle Settings Dialog
- Daily Log Repository
- Hydration Status Pill
- Log History ViewModel
- Log Day Summary UI
- pH Insight Logic Tests
- Symptom Pattern Tests
- Test Coroutine Dispatcher Rule
- Reminders Feature Overview
- Genesyx Product Repository
- Daily Log DAO
- pH Tracker Card UI
- Hydration Coach Tests
- Reminder Notifier Instrumented Test
- Feature Flags & Nav Graph
- pH Remote Data Source
- Learn Article Content
- Hydration Unit Preferences
- Home ViewModel
- Nutrition ViewModel
- Pregnancy Screen
- Preferences DataStore Tests
- Ovulation Logic Tests
- Sleep Insight Logic Tests
- Cycle Repository & Tracker Summary
- Cycle Settings DAO
- User Supplement Repository
- Cycle Engine Utilities
- Quiz Answers Remote Data
- Theme Mode Setting
- Sleep Detail ViewModel Tests
- pH Tracker Section
- Design Docs & Web Prototype
- Quiz Answers ViewModel
- Notification Permission Tests
- Supabase Schema SQL
- Cycle & Waitlist Remote Data
- Meal Entry DAO
- pH Reading Range Tests
- Daily Log Sync Scheduler
- pH Sync Scheduler
- App Init & Notification Channels
- Main Activity & Bottom Nav
- Daily Log DTO Intimacy Tests
- Learn Section & Articles
- Cycle Tracking Core
- Streak Repository & Milestones
- Boot Reminder Rescheduling
- Reminder Notification Worker
- Cycle Regularity Logic
- Cycle Detail Screen
- Hydration Detail Screen
- pH Education Copy Tests
- Tracking Vector Tests
- Google Sign-in Error Handling
- Daily Log & Nutrition Tracking
- Hydration Coaching & Formatting
- Supplement Reminder Worker
- Daily Log ViewModel
- Supplement Sync Scheduling
- Supplement Vocabulary Tests
- pH Copy Verbatim Tests
- Supplement Reminder Scheduling Tests
- Hilt Test Runner
- Supplement Reminder Notification Test
- Supplement Reminder Scheduler
- pH Sync Worker
- Supplement Sync Worker
- Hydration Coaching Logic
- Supplement Time-of-Day Model
- Sync Status Repository Tests
- Supplement Log Rows Tests
- Week Bucket Tests
- Log ViewModel Date Tests
- Backend API Functions
- Supplement & Hydration Insights
- Supplement Sync Scheduler
- Learn Section ViewModel
- Waitlist Join Flow
- pH & Log History Test Data
- Sync Status Repository
- Past Date Picker Dialog
- Hydration Display Format Tests
- User Supplement Wire Tests
- Daily Log Room Migrations
- pH Room Migrations
- Toolchain Smoke Test
- Supplement Log Rows Logic
- App Foreground Tracking
- Reminder Kinds & Content
- Supplement Supabase Migration
- Gradle Wrapper Script
- pH Copy Banned Phrase Test
- Release Testing Docs & Auth Screen
- Food Plates Flat-Lay Photo
- Bedside Water Photo
- Dirt Path Photo
- Water Pouring Photo
- Line Traces Graphic
- Notebook Writing Photo
- Open Palm Photo
- Trend Line Graphic
- Brass Dial Photo
- Water Glass Flat-Lay Photo
- CLAUDE.md Preflight Corrections
- Worklog Entry July 14
- Worklog v1.2 Build
- Worklog Bug-fix Batch

## God Nodes (most connected - your core abstractions)
1. `DailyLog` - 134 edges
2. `DataResult` - 129 edges
3. `Eyebrow()` - 72 edges
4. `CycleSettings` - 64 edges
5. `PhReading` - 51 edges
6. `HydrationUnit` - 46 edges
7. `GxPrimaryButton()` - 44 edges
8. `PreferencesRepository` - 42 edges
9. `GenesyxPreferencesDataStore` - 41 edges
10. `Screen` - 40 edges

## Surprising Connections (you probably didn't know these)
- `Genesyx wordmark logo` --conceptually_related_to--> `Design Tokens Doc`  [AMBIGUOUS]
  app/src/main/res/drawable-nodpi/logo_g.png → docs/DESIGN_TOKENS.md
- `Home screen hero background (floating orb blobs)` --conceptually_related_to--> `APP_INVENTORY.md — Repository and App Inventory`  [INFERRED]
  app/src/main/res/drawable-nodpi/home_hero_bg.jpg → APP_INVENTORY.md
- `Data Layer Doc` --references--> `account.functions.ts (Account API)`  [EXTRACTED]
  docs/DATA_LAYER.md → account.functions.ts
- `Data Layer Doc` --references--> `cycle.functions.ts (Cycle Settings API)`  [EXTRACTED]
  docs/DATA_LAYER.md → cycle.functions.ts
- `Data Layer Doc` --references--> `daily-log.functions.ts (Daily Log API)`  [EXTRACTED]
  docs/DATA_LAYER.md → daily-log.functions.ts

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Cross-Platform Tracking Vector Contract** — app_domain_tracking_tracking_test_vectors, docs_worklog_2026_07_28_phase_a_cycle, docs_worklog_2026_07_28_phase_e_p1_fixes, docs_worklog_2026_07_16_weekly_summary [EXTRACTED 0.75]
- **Cross-Screen Single-Source-of-Truth Bug Batch** — docs_worklog_2026_07_28_phase_a_cycle, docs_worklog_2026_07_28_phase_b_hydration, docs_worklog_2026_07_28_phase_c_sleep, docs_worklog_2026_07_28_phase_d_ph_latest [EXTRACTED 0.85]
- **Cross-referenced release/status documents** — claude_overview, changelog_overview, app_inventory_overview, final_report_overview, launch_week_overview, manual_test_checklist_overview, autonomous_run_log_overview [EXTRACTED 0.85]
- **Genesyx brand and splash visual assets** — app_src_main_assets_genesyx_logo_svg, app_src_main_res_drawable_nodpi_brand_lockup_image, app_src_main_res_drawable_nodpi_brand_lockup_dark_image, app_src_main_res_drawable_nodpi_ic_launcher_foreground_image, app_src_main_res_drawable_nodpi_ic_launcher_monochrome_image, app_src_main_res_drawable_nodpi_egg_female_image, app_src_main_res_drawable_nodpi_egg_male_image [INFERRED 0.65]
- **pH / Fertility Editorial Guardrail** — docs_v1_1_ph_wellness_article_cluster, app_domain_content_quiz_content, claude_md [INFERRED 0.65]
- **Learn Section Architecture** — docs_v1_1_learn_feature, docs_v1_1_learn_content_model, docs_v1_1_article_bookmark_entity, docs_v1_1_ph_wellness_article_cluster, docs_v1_1_learn_bottom_nav_decision [INFERRED 0.75]
- **Reminders Feature Architecture** — docs_v1_1_reminders_feature, docs_v1_1_notification_channels, docs_v1_1_reminder_scheduler, docs_v1_1_reminder_worker, docs_v1_1_reminder_notifier, docs_v1_1_notification_permission, docs_v1_1_deep_linking [INFERRED 0.75]
- **Vaginal pH tracking feature evolution across docs** — concept_ph_tracking, concept_ph_value_range_constraint, concept_room_database, concept_supabase_backend, claude_overview, changelog_overview [INFERRED 0.75]

## Communities (174 total, 62 thin omitted)

### Community 0 - "Supplement Remote Data Sources"
Cohesion: 0.15
Nodes (9): GenesyxProductDto, UserSupplementDto, GenesyxProductRemoteDataSource, UserSupplementRemoteDataSource, StubGenesyxProductRemoteDataSource, StubUserSupplementRemoteDataSource, StubWaitlistRemoteDataSource, SupabaseGenesyxProductRemoteDataSource (+1 more)

### Community 1 - "Symptom & Hydration Insights"
Cohesion: 0.07
Nodes (6): DailyLog, SymptomPatternLogic, HydrationInsightLogicTest, WeeklySummaryLogicTest, TrackerSummaryLogicTest, TrackViewModelTest

### Community 2 - "Auth Service"
Cohesion: 0.12
Nodes (7): AuthService, AuthSession, LocalAuthService, SupabaseAuthService, DataResult, Error, Loading

### Community 3 - "Repo Docs & Brand Assets"
Cohesion: 0.13
Nodes (40): APP_INVENTORY.md — Repository and App Inventory, Genesyx logo SVG asset, Genesyx brand lockup (dark-mode variant), Genesyx brand lockup (light) — wordmark + icon, Splash egg graphic (pink/purple gradient), Splash egg graphic (blue gradient), Home screen hero background (floating orb blobs), App launcher icon foreground (G monogram) (+32 more)

### Community 4 - "Reminder Policy Core"
Cohesion: 0.21
Nodes (4): NotificationSettings, PostContext, ReminderPolicy, java

### Community 5 - "Profile ViewModel"
Cohesion: 0.09
Nodes (6): FocusMode, PREGNANCY, PREP, StateFlow, ViewModel, ProfileViewModel

### Community 6 - "Notification Permission Logic"
Cohesion: 0.09
Nodes (29): Activity, NotificationPermission, PushPermissionStatus, BLOCKED_IN_SETTINGS, DENIED_PERMANENT, DENIED_SOFT, GRANTED, NOT_ASKED (+21 more)

### Community 7 - "Onboarding Quiz UI"
Cohesion: 0.12
Nodes (29): DidYouKnow, QuizOption, QuizQuestion, BrandLockup(), GxGhostButton(), GxOptionPill(), GxPrimaryButton(), Color (+21 more)

### Community 8 - "pH Repository"
Cohesion: 0.15
Nodes (6): Accepted, StateFlow, OutOfRange, PhRepository, PhWriteResult, PhRepositoryTest

### Community 10 - "Daily Log Repository Tests"
Cohesion: 0.10
Nodes (6): DailyLogRepositoryTest, FakeRemote, CoroutineScope, RecordingScheduler, SilentLogger, FakeRemote

### Community 11 - "Client Repository & Results"
Cohesion: 0.07
Nodes (21): getOrNull(), map(), runCatchingResult(), Success, ClientRepository, Flow, ClientDao, Flow (+13 more)

### Community 13 - "Meal Log UI & Data"
Cohesion: 0.06
Nodes (34): MealLogCardTest, RecipesSectionTest, toDomain(), toEntity(), Flow, MealLogRepository, Recipe, recipesFor() (+26 more)

### Community 14 - "App Config & DI Core"
Cohesion: 0.10
Nodes (12): Environment, DEV, PROD, STAGING, CoreModule, CoroutineScope, DispatcherProvider, DefaultDispatcherProvider (+4 more)

### Community 15 - "User Supplement DAO"
Cohesion: 0.11
Nodes (12): Flow, UserSupplementDao, SupplementSyncStatus, PENDING_DELETE, PENDING_UPSERT, SYNCED, toDomain(), toEntity() (+4 more)

### Community 17 - "Nutrition Screen UI"
Cohesion: 0.20
Nodes (18): PhaseFood, Eyebrow(), ArticlesSection(), FocusFoodsCard(), HydrationCard(), androidx, Color, NavController (+10 more)

### Community 18 - "Daily Log Domain Models"
Cohesion: 0.11
Nodes (12): EnergyLevel, HIGH, LOW, NORMAL, isMeaningful(), Mood, GOOD, GREAT (+4 more)

### Community 19 - "Insights Screen UI"
Cohesion: 0.10
Nodes (44): AvgTile(), BarsCard(), ConsistencyCard(), CyclePhaseTimelineCard(), EmptyInsightsCard(), HydrationCard(), InsightsCard(), InsightsScreen() (+36 more)

### Community 20 - "UI/DB Spec Inventory"
Cohesion: 0.10
Nodes (27): AppShell (spec), BottomTabBar (spec), CycleSettingsDialog (spec), PartnerSection (spec), PhLogDialog (spec), PhTrackerCard (spec), ThemeToggle (spec), Auth Screen (+19 more)

### Community 21 - "Partner Repository"
Cohesion: 0.21
Nodes (9): toDomain(), StateFlow, PartnerRepository, InviteStatus, ACCEPTED, PENDING, REVOKED, Partner (+1 more)

### Community 22 - "Cycle Engine Models"
Cohesion: 0.12
Nodes (14): CyclePhaseInfo, DayType, FERTILE, FOLLICULAR, LUTEAL, OVULATION, PERIOD, FertileWindow (+6 more)

### Community 24 - "Navigation Screens"
Cohesion: 0.06
Nodes (27): ArticleDetail, Auth, Clients, CycleDetail, Home, HydrationDetail, Insights, Invite (+19 more)

### Community 25 - "Supplement Insight Logic"
Cohesion: 0.15
Nodes (9): Supplement, FOLATE, IRON, OMEGA_3, VITAMIN_D, ZINC, SupplementInsightLogic, TrackerSummary (+1 more)

### Community 26 - "Home Screen UI"
Cohesion: 0.21
Nodes (19): PastDatePickerDialog(), CycleHeroCard(), FirstRunSetupCard(), HeroMetric(), HomeContent(), HomeContentEmptyPreview(), HomeScreen(), HydrationChallengeCard() (+11 more)

### Community 27 - "Room Type Converters"
Cohesion: 0.10
Nodes (8): Converters, LogSyncStatus, PENDING_UPSERT, SYNCED, PhSyncStatus, PENDING_DELETE, PENDING_UPSERT, SYNCED

### Community 28 - "Notification Settings Repository"
Cohesion: 0.12
Nodes (3): Keys, Flow, NotificationSettingsRepository

### Community 29 - "App Navigation Start State"
Cohesion: 0.36
Nodes (4): AppViewModel, StateFlow, ViewModel, AppViewModelTest

### Community 30 - "Clients & Log Screens"
Cohesion: 0.19
Nodes (19): AddClientDialog(), ClientCard(), ClientsScreen(), EmptyState(), Modifier, ScreenHeader(), Color, ImageVector (+11 more)

### Community 31 - "Cycle Content & Phase Copy"
Cohesion: 0.15
Nodes (8): FocusCopy, FocusFood, PhaseHeroCopy, phaseHeroSubtext(), phaseHeroText(), phaseSubLabel(), phaseTags(), NutritionContentTest

### Community 32 - "Streak Engine & DataStore"
Cohesion: 0.20
Nodes (6): Flow, DataStoreModule, Context, StreakEngine, DataStore, Preferences

### Community 33 - "Auth Screen UI"
Cohesion: 0.19
Nodes (13): GoogleCredentialClient, Context, AuthContent(), AuthContentDarkPreview(), AuthScreen(), AuthUiState, AuthViewModel, Field() (+5 more)

### Community 34 - "pH Reading DAO"
Cohesion: 0.15
Nodes (8): Flow, PhReadingDao, PhReadingEntity, toDomain(), toEntity(), parseTs(), toDto(), toEntity()

### Community 35 - "Profile Remote Data Source"
Cohesion: 0.14
Nodes (5): ProfileDto, ProfileRemoteDataSource, RemoteProfile, StubProfileRemoteDataSource, SupabaseProfileRemoteDataSource

### Community 37 - "Brand UI & pH Status"
Cohesion: 0.32
Nodes (4): SupplementPlanItem, BrandOrb(), Dp, Modifier

### Community 39 - "pH Copy & Citations"
Cohesion: 0.14
Nodes (10): Citation, PhCopy, PhStatus, ELEVATED, HEALTHY, CitationList(), Modifier, androidx (+2 more)

### Community 40 - "Consistency Insight Logic"
Cohesion: 0.27
Nodes (3): StreakState, ConsistencyInsightLogic, ConsistencyInsightLogicTest

### Community 41 - "Profile Screen UI"
Cohesion: 0.20
Nodes (15): AppLinks, isValidEmail(), CardGroup(), ChangeEmailDialog(), ChangePasswordDialog(), DetailLine(), Divider(), EditNameDialog() (+7 more)

### Community 43 - "Partner Invite DAO"
Cohesion: 0.19
Nodes (6): Flow, PartnerDao, PartnerInviteEntity, toEntity(), PartnerLinkEntity, RoomDatabase

### Community 45 - "Legacy File Path Refs"
Cohesion: 0.14
Nodes (8): CLAUDE.md Project Instructions, iOS Learn Parity Handoff, Learn Feature Audit, v1.1 Notifications & Learn Brief, Default-ON Reminder Toggle Defect, Notifications Gap Matrix (Part B), Notifications Audit (Part A), feature/partner-invites Branch

### Community 46 - "Profile DAO & Repository"
Cohesion: 0.23
Nodes (5): Flow, ProfileDao, ProfileEntity, Flow, ProfileRepository

### Community 47 - "Learn Content Tests"
Cohesion: 0.10
Nodes (3): articleBySlug(), LearnContentTest, LearnDripTest

### Community 48 - "Sleep Detail Screen"
Cohesion: 0.22
Nodes (12): SleepInsightLogic, ImageVector, Modifier, StateFlow, ViewModel, SleepBars(), SleepDetailScreen(), SleepDetailState (+4 more)

### Community 49 - "Meal Log Migration & DB Module"
Cohesion: 0.21
Nodes (4): MealLogMigrationTest, GenesyxDatabase, DatabaseModule, Context

### Community 52 - "Auth Repository Tests"
Cohesion: 0.25
Nodes (5): AuthUser, AuthRepositoryTest, DispatcherProvider, CoroutineScope, DispatcherProvider

### Community 53 - "Cloud API & Analytics DI"
Cohesion: 0.21
Nodes (5): CloudApi, DefaultCloudApi, Analytics, NoopAnalytics, BindingsModule

### Community 54 - "Daily Log Remote Data"
Cohesion: 0.15
Nodes (4): DailyLogDto, DailyLogRemoteDataSource, StubDailyLogRemoteDataSource, SupabaseDailyLogRemoteDataSource

### Community 55 - "Reminder Settings ViewModel"
Cohesion: 0.24
Nodes (3): StateFlow, ViewModel, ReminderSettingsViewModel

### Community 56 - "Track Calendar UI"
Cohesion: 0.19
Nodes (17): CalendarCell, Day, Empty, DayCell(), DayDetailDialog(), EmptyCalendar(), androidx, Color (+9 more)

### Community 58 - "Cycle Settings Dialog"
Cohesion: 0.22
Nodes (9): CycleSettingsDialogTest, CycleSettingsDialog(), SelectableDates, androidx, SelectableDates, NumberStepper(), StepperButton(), toUtcMillis() (+1 more)

### Community 59 - "Daily Log Repository"
Cohesion: 0.16
Nodes (6): DailyLogRepository, StateFlow, DailyLogSyncEntryPoint, DailyLogSyncWorker, CoroutineWorker, Result

### Community 60 - "Hydration Status Pill"
Cohesion: 0.20
Nodes (15): HydrationPace, AHEAD, BEHIND, NOT_STARTED, ON_TRACK, REACHED, WELL_BEHIND, hydrationStatusColor() (+7 more)

### Community 61 - "Log History ViewModel"
Cohesion: 0.20
Nodes (9): LogDay, StateFlow, ViewModel, LogHistoryViewModel, DayMarker, ACTIVITY, PH, SYMPTOMS (+1 more)

### Community 62 - "Log Day Summary UI"
Cohesion: 0.27
Nodes (12): DailyLogSummary(), InfoRow(), Modifier, label(), PhReadingRow(), sleepLabel(), dateLabel(), EmptyState() (+4 more)

### Community 65 - "Test Coroutine Dispatcher Rule"
Cohesion: 0.43
Nodes (3): MainDispatcherRule, Description, TestWatcher

### Community 66 - "Reminders Feature Overview"
Cohesion: 0.18
Nodes (10): Reminder/Learn Deep Linking, Notification Channels, POST_NOTIFICATIONS Permission State Machine, NotificationSettings Model, ReminderKind Enum, ReminderNotifier / shouldPost, ReminderScheduler, ReminderWorker (+2 more)

### Community 67 - "Genesyx Product Repository"
Cohesion: 0.24
Nodes (7): GenesyxProductRepository, toDomain(), GenesyxProduct, formatMinutes(), GenesyxRangeCard(), UserSupplementDialog(), UserSupplementsCard()

### Community 68 - "Daily Log DAO"
Cohesion: 0.24
Nodes (5): DailyLogDao, Flow, DailyLogEntity, toDomain(), toEntity()

### Community 69 - "pH Tracker Card UI"
Cohesion: 0.26
Nodes (12): ChartEmpty(), EmptyState(), Modifier, LatestReadingPanel(), PhChart(), PhRange, ALL, MONTH (+4 more)

### Community 71 - "Reminder Notifier Instrumented Test"
Cohesion: 0.19
Nodes (5): Context, NotificationManager, ReminderNotifierInstrumentedTest, StateFlow, SessionRepository

### Community 72 - "Feature Flags & Nav Graph"
Cohesion: 0.11
Nodes (25): FeatureFlags, ExpandableInfo(), Modifier, GenesyxNavGraph(), Modifier, NavHostController, Benefit, OnboardingIntroScreen() (+17 more)

### Community 73 - "pH Remote Data Source"
Cohesion: 0.26
Nodes (4): PhReadingDto, PhRemoteDataSource, StubPhRemoteDataSource, SupabasePhRemoteDataSource

### Community 74 - "Learn Article Content"
Cohesion: 0.08
Nodes (45): Article, ArticleBlock, ArticleCategory, GETTING_STARTED, GUIDES, INSIGHTS, NUTRITION, TRACKING (+37 more)

### Community 75 - "Hydration Unit Preferences"
Cohesion: 0.13
Nodes (12): StateFlow, HydrationFormat, HydrationUnit, CUPS, ML, GoalStepper(), HydrationGoalDialog(), Color (+4 more)

### Community 76 - "Home ViewModel"
Cohesion: 0.24
Nodes (5): HomeViewModel, StateFlow, ViewModel, SessionAndLearn, com

### Community 77 - "Nutrition ViewModel"
Cohesion: 0.21
Nodes (4): StateFlow, ViewModel, NutritionUiState, NutritionViewModel

### Community 78 - "Pregnancy Screen"
Cohesion: 0.32
Nodes (11): FeatureCard(), androidx, ImageVector, Modifier, StateFlow, ViewModel, PregnancyHome(), PregnancyScreen() (+3 more)

### Community 82 - "Cycle Repository & Tracker Summary"
Cohesion: 0.18
Nodes (8): CycleRepository, StateFlow, CycleSettings, emptyTrackerSummaries(), TrackerSummaries, StateFlow, ViewModel, TrackViewModel

### Community 83 - "Cycle Settings DAO"
Cohesion: 0.25
Nodes (5): CycleSettingsDao, Flow, CycleSettingsEntity, toDomain(), toEntity()

### Community 84 - "User Supplement Repository"
Cohesion: 0.24
Nodes (6): Accepted, InvalidName, StateFlow, SupplementWriteResult, UserSupplementRepository, UserSupplement

### Community 86 - "Quiz Answers Remote Data"
Cohesion: 0.11
Nodes (6): StateFlow, QuizAnswersDto, QuizAnswersRemoteDataSource, StubQuizAnswersRemoteDataSource, SupabaseQuizAnswersRemoteDataSource, QuizAnswersDtoTest

### Community 87 - "Theme Mode Setting"
Cohesion: 0.29
Nodes (4): ThemeMode, DARK, LIGHT, SYSTEM

### Community 89 - "pH Tracker Section"
Cohesion: 0.35
Nodes (6): Modifier, StateFlow, ViewModel, PhHistoryCard(), PhTrackerSection(), PhTrackerViewModel

### Community 90 - "Design Docs & Web Prototype"
Cohesion: 0.25
Nodes (11): Genesyx wordmark logo, Architecture Doc, Cycle Engine Doc, Design Tokens Doc, Screen Layouts Doc, UI/UX Spec, mockData.ts (Seed/Mock Content), use-ph.ts (pH Logic) (+3 more)

### Community 91 - "Quiz Answers ViewModel"
Cohesion: 0.19
Nodes (5): QuizAnswersRepository, ViewModel, OnboardingQuizViewModel, FakeRemote, QuizAnswersRepositoryTest

### Community 93 - "Supabase Schema SQL"
Cohesion: 0.18
Nodes (6): public.cycle_settings, public.daily_logs, public.partner_invites, public.ph_readings, public.profiles, public.waitlist_emails

### Community 94 - "Cycle & Waitlist Remote Data"
Cohesion: 0.13
Nodes (10): AppConfig, CycleSettingsDto, CycleRemoteDataSource, WaitlistRemoteDataSource, StubCycleRemoteDataSource, SupabaseCycleRemoteDataSource, SupabaseWaitlistRemoteDataSource, NetworkModule (+2 more)

### Community 95 - "Meal Entry DAO"
Cohesion: 0.31
Nodes (3): Flow, MealEntryDao, MealEntryEntity

### Community 97 - "Daily Log Sync Scheduler"
Cohesion: 0.31
Nodes (3): DailyLogSyncScheduler, ExistingWorkPolicy, WorkManagerDailyLogSyncScheduler

### Community 98 - "pH Sync Scheduler"
Cohesion: 0.29
Nodes (3): ExistingWorkPolicy, PhSyncScheduler, WorkManagerPhSyncScheduler

### Community 99 - "App Init & Notification Channels"
Cohesion: 0.27
Nodes (6): GenesyxApplication, Application, Context, NotificationChannels, NotificationChannelCompat, R

### Community 100 - "Main Activity & Bottom Nav"
Cohesion: 0.21
Nodes (8): Intent, NavHostController, MainActivity, BottomNavItem, GenesyxBottomNav(), NavController, Bundle, ComponentActivity

### Community 102 - "Learn Section & Articles"
Cohesion: 0.25
Nodes (6): ArticleBookmarkEntity (Room), Six-Tab Bottom Nav Decision, Article Content Model, Learn Section Feature, pH Wellness Article Cluster, Phase D: pH Latest-Reading Fix

### Community 103 - "Cycle Tracking Core"
Cohesion: 0.25
Nodes (3): Home/Track/Insights iOS Parity Build, Weekly Summary Card (Insights), Phase A: Cycle Single Source of Truth

### Community 104 - "Streak Repository & Milestones"
Cohesion: 0.24
Nodes (9): StateFlow, StreakRepository, Milestone, DAY_14, DAY_7, WEEK_1, WEEK_4, MilestoneDialog() (+1 more)

### Community 105 - "Boot Reminder Rescheduling"
Cohesion: 0.36
Nodes (5): BootEntryPoint, BootReceiver, Context, Intent, BroadcastReceiver

### Community 106 - "Reminder Notification Worker"
Cohesion: 0.33
Nodes (4): CoroutineWorker, Result, ReminderEntryPoint, ReminderWorker

### Community 108 - "Cycle Detail Screen"
Cohesion: 0.42
Nodes (7): CycleDetailScreen(), CycleDetailViewModel, DetailCard(), Modifier, StateFlow, ViewModel, Tile()

### Community 109 - "Hydration Detail Screen"
Cohesion: 0.31
Nodes (4): HydrationDetailState, HydrationDetailViewModel, StateFlow, ViewModel

### Community 113 - "Daily Log & Nutrition Tracking"
Cohesion: 0.32
Nodes (3): SeedTestData Manual Seeder, Phase B: Hydration Single Source of Truth, Phase C: Sleep Editor Fix

### Community 114 - "Hydration Coaching & Formatting"
Cohesion: 0.29
Nodes (5): Intraday Hydration Coaching, Google Sign-in Debug-Build Triage, ml/cups Display Preference, Phase E: Pace Tiers, Honest Average, One Formatter, Banner Re-verify, Phase F: P2 Polish (pH Chart, Slider, Legacy Labels)

### Community 115 - "Supplement Reminder Worker"
Cohesion: 0.36
Nodes (4): CoroutineWorker, Result, SupplementReminderEntryPoint, SupplementReminderWorker

### Community 116 - "Daily Log ViewModel"
Cohesion: 0.32
Nodes (3): StateFlow, ViewModel, LogViewModel

### Community 122 - "Hilt Test Runner"
Cohesion: 0.43
Nodes (5): AndroidJUnitRunner, HiltTestRunner, Application, Context, ClassLoader

### Community 123 - "Supplement Reminder Notification Test"
Cohesion: 0.38
Nodes (3): Context, NotificationManager, SupplementReminderWorkerInstrumentedTest

### Community 124 - "Supplement Reminder Scheduler"
Cohesion: 0.19
Nodes (3): StateFlow, SupplementReminderRepository, SupplementReminderScheduler

### Community 126 - "pH Sync Worker"
Cohesion: 0.38
Nodes (4): CoroutineWorker, Result, PhSyncEntryPoint, PhSyncWorker

### Community 127 - "Supplement Sync Worker"
Cohesion: 0.38
Nodes (4): CoroutineWorker, Result, UserSupplementSyncEntryPoint, UserSupplementSyncWorker

### Community 131 - "Supplement Time-of-Day Model"
Cohesion: 0.29
Nodes (5): SupplementTime, AFTERNOON, ANYTIME, EVENING, MORNING

### Community 137 - "Backend API Functions"
Cohesion: 0.40
Nodes (6): account.functions.ts (Account API), cycle.functions.ts (Cycle Settings API), daily-log.functions.ts (Daily Log API), Data Layer Doc, partner.functions.ts (Partner Invite API), ph.functions.ts (pH Reading API)

### Community 138 - "Supplement & Hydration Insights"
Cohesion: 0.33
Nodes (4): PR #9 (v1.1) Merge, Phase 2: Supplement Card + Hydration-Goal Bug Fix, Phase 0: Supplement Vocabulary, Phase 1: WeekBuckets Unification

### Community 141 - "Learn Section ViewModel"
Cohesion: 0.47
Nodes (3): StateFlow, ViewModel, LearnViewModel

### Community 142 - "Waitlist Join Flow"
Cohesion: 0.47
Nodes (3): StateFlow, ViewModel, WaitlistViewModel

### Community 143 - "pH & Log History Test Data"
Cohesion: 0.18
Nodes (5): SeedTestData, PhMeasurement, PhReading, PhInsightLogic, LogHistoryViewModelTest

### Community 154 - "Reminder Kinds & Content"
Cohesion: 0.13
Nodes (13): Channels, ReminderKind, DAILY_LOG, FERTILE_WINDOW, HYDRATION, MISSED_LOG, NEW_ARTICLE, NUTRITION (+5 more)

### Community 158 - "Gradle Wrapper Script"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

## Ambiguous Edges - Review These
- `account.functions.ts (Account API)` → `ph.functions.ts (pH Reading API)`  [AMBIGUOUS]
  docs/DATA_LAYER.md · relation: conceptually_related_to
- `join_waitlist SECURITY DEFINER RPC` → `MANUAL_TEST_CHECKLIST.md — v1.1 manual QA checklist`  [AMBIGUOUS]
  MANUAL_TEST_CHECKLIST.md · relation: references
- `Design Tokens Doc` → `Genesyx wordmark logo`  [AMBIGUOUS]
  docs/DESIGN_TOKENS.md · relation: conceptually_related_to
- `pH Wellness Article Cluster` → `Phase D: pH Latest-Reading Fix`  [AMBIGUOUS]
  docs/worklog/2026-07-28.md · relation: conceptually_related_to
- `Notifications Gap Matrix (Part B)` → `v1.1 Notifications & Learn Brief`  [AMBIGUOUS]
  docs/worklog/2026-07-14.md · relation: references

## Knowledge Gaps
- **200 isolated node(s):** `Keys`, `BottomNavItem`, `Error`, `Loading`, `Day` (+195 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **62 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `account.functions.ts (Account API)` and `ph.functions.ts (pH Reading API)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `join_waitlist SECURITY DEFINER RPC` and `MANUAL_TEST_CHECKLIST.md — v1.1 manual QA checklist`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **What is the exact relationship between `Design Tokens Doc` and `Genesyx wordmark logo`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `pH Wellness Article Cluster` and `Phase D: pH Latest-Reading Fix`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Notifications Gap Matrix (Part B)` and `v1.1 Notifications & Learn Brief`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **Why does `DailyLog` connect `Symptom & Hydration Insights` to `Supplement Remote Data Sources`, `Streak Engine Tests`, `Daily Log Repository Tests`, `pH & Log History Test Data`, `Daily Log Domain Models`, `Supplement Insight Logic`, `Clients & Log Screens`, `Streak Engine & DataStore`, `Sleep Detail Screen`, `Supplement Insight Logic Tests`, `Daily Log Remote Data`, `Day Markers Tests`, `Daily Log Repository`, `Log History ViewModel`, `Log Day Summary UI`, `Symptom Pattern Tests`, `Test Coroutine Dispatcher Rule`, `Daily Log DAO`, `Hydration Unit Preferences`, `Sleep Insight Logic Tests`, `Cycle Repository & Tracker Summary`, `Sleep Detail ViewModel Tests`, `Tracking Vector Tests`, `Daily Log ViewModel`?**
  _High betweenness centrality (0.197) - this node is a cross-community bridge._
- **Why does `DataResult` connect `Auth Service` to `Supplement Remote Data Sources`, `Profile ViewModel`, `pH Repository`, `Client Repository & Results`, `Waitlist Join Flow`, `App Config & DI Core`, `User Supplement DAO`, `Streak Engine & DataStore`, `Auth Screen UI`, `Profile Remote Data Source`, `Auth Repository`, `Profile DAO & Repository`, `Daily Log Remote Data`, `Daily Log Repository`, `Genesyx Product Repository`, `pH Remote Data Source`, `Cycle Repository & Tracker Summary`, `User Supplement Repository`, `Quiz Answers Remote Data`, `Quiz Answers ViewModel`, `Cycle & Waitlist Remote Data`, `pH Sync Scheduler`?**
  _High betweenness centrality (0.099) - this node is a cross-community bridge._