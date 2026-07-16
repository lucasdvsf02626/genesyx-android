package com.genesyx.app

import com.genesyx.app.data.CycleRepository
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.PhRepository
import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.EnergyLevel
import com.genesyx.app.domain.model.Mood
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.model.Supplement
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import javax.inject.Inject

/**
 * Not a test — a manual re-seeder. Writes a realistic guest (signed-out) dataset through the real
 * repositories, which persist to the same Room/DataStore the app reads, so launching the app after
 * this shows populated Home/Track/Insights. Run with:
 *   ./gradlew :app:connectedDebugAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=com.genesyx.app.SeedTestData
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SeedTestData {

    @get:Rule val hilt = HiltAndroidRule(this)

    @Inject lateinit var cycle: CycleRepository
    @Inject lateinit var logs: DailyLogRepository
    @Inject lateinit var ph: PhRepository
    @Inject lateinit var prefs: PreferencesRepository

    @Before fun setup() = hilt.inject()

    @Test
    fun seed() = runBlocking<Unit> {
        val today = LocalDate.now()
        prefs.setHydrationGoalMl(2400)
        cycle.upsert(CycleSettings(lastPeriodDate = today.minusDays(8), cycleLength = 28, periodLength = 5))

        val week = mapOf(
            today to DailyLog(
                waterMl = 800,
                mood = Mood.GOOD,
                energy = EnergyLevel.NORMAL,
                supplements = setOf(Supplement.FOLATE.wireName, Supplement.VITAMIN_D.wireName, Supplement.OMEGA_3.wireName),
            ),
            today.minusDays(1) to DailyLog(waterMl = 2400, sleepMinutes = 440, mood = Mood.GREAT, energy = EnergyLevel.HIGH),
            today.minusDays(2) to DailyLog(waterMl = 2600, sleepMinutes = 480, supplements = setOf(Supplement.FOLATE.wireName)),
            today.minusDays(3) to DailyLog(waterMl = 1500, symptoms = setOf("Cramps", "Bloating"), mood = Mood.OKAY),
            today.minusDays(4) to DailyLog(waterMl = 2400, sleepMinutes = 420),
            today.minusDays(5) to DailyLog(waterMl = 2000),
            today.minusDays(6) to DailyLog(waterMl = 2400, sleepMinutes = 450),
        )
        week.forEach { (date, log) -> logs.upsert(date, log) }

        ph.create(PhReading(phValue = 6.5, recordedAt = today.minusDays(4).atTime(9, 0)))
        ph.create(PhReading(phValue = 7.0, recordedAt = today.minusDays(2).atTime(8, 30)))
        ph.create(PhReading(phValue = 6.8, recordedAt = today.atTime(8, 0)))

        // Wait for the async writes to actually land in Room (subscribing drives the WhileSubscribed flow).
        withTimeout(10_000) { logs.logByDate.first { it.size >= week.size } }
        withTimeout(10_000) { ph.readings.first { it.size >= 3 } }
        withTimeout(10_000) { cycle.settings.first { it != null } }
    }
}
