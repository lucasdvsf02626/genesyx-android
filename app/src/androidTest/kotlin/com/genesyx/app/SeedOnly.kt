package com.genesyx.app

/**
 * Marks an instrumented "test" that is really a manual utility (e.g. [SeedTestData]) and must never
 * run in a gradle / CI sweep. `app/build.gradle.kts` passes `notAnnotation = com.genesyx.app.SeedOnly`
 * to the runner, so every gradle-driven `connectedAndroidTest` skips these — even when a class filter
 * names one. An explicit `adb shell am instrument -e class ...` does not pass that argument, so the
 * utility stays runnable by hand.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class SeedOnly
