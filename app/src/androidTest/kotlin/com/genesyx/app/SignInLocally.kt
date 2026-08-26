package com.genesyx.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.genesyx.app.data.SessionRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Manual utility, never run by gradle ([SeedOnly]): puts the installed app into a *local* signed-in
 * state — no Supabase account, no network — so the real MainActivity opens on Home for on-device
 * QA and screenshots. Pair with [SeedTestData] for a week of logs. Run by hand:
 *
 * `adb shell am instrument -w -e class com.genesyx.app.SignInLocally com.genesyx.app.test/com.genesyx.app.HiltTestRunner`
 */
@HiltAndroidTest
@SeedOnly
@RunWith(AndroidJUnit4::class)
class SignInLocally {

    @get:Rule val hilt = HiltAndroidRule(this)

    @Inject lateinit var session: SessionRepository

    @Before fun setup() = hilt.inject()

    @Test
    fun signIn() = runBlocking<Unit> {
        session.signIn(email = "qa-local@example.com", name = "QA Tester", userId = "qa-local-user")
        withTimeout(10_000) { while (!session.awaitSignedIn()) delay(50) }
    }
}
