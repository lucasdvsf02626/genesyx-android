package com.genesyx.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Proves the instrumented toolchain: HiltTestRunner -> HiltTestApplication -> injection. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ToolchainSmokeTest {
    @get:Rule val hilt = HiltAndroidRule(this)

    @Before fun setup() = hilt.inject()

    @Test fun app_context_has_correct_package() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("com.genesyx.app", ctx.packageName)
    }
}
