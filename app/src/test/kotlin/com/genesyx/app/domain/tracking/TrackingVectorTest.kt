package com.genesyx.app.domain.tracking

import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.EnergyLevel
import com.genesyx.app.domain.model.Mood
import com.genesyx.app.domain.streaks.StreakEngine
import com.genesyx.app.ui.insights.HydrationInsightLogic
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Runs [StreakEngine] against `tracking_test_vectors.json` — the cross-platform contract. The same
 * file is mirrored verbatim into the iOS repo, so both platforms are pinned to identical numbers for
 * identical inputs; a metric that drifts on one of them fails here.
 *
 * The vectors were derived from the tracking spec independently of this engine, so they are a real
 * check rather than a restatement of whatever the Kotlin happens to do. If a vector and the engine
 * disagree, the spec wins and the **engine** changes.
 */
class TrackingVectorTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val doc: JsonObject by lazy {
        val stream = javaClass.classLoader!!.getResourceAsStream("tracking_test_vectors.json")
            ?: error(
                "tracking_test_vectors.json is not on the test classpath. It lives beside the engine " +
                    "in domain/tracking and is wired in via the test resources srcDir in build.gradle.kts.",
            )
        json.parseToJsonElement(stream.bufferedReader().readText()).jsonObject
    }

    private val cases get() = doc["cases"]!!.jsonArray.map { it.jsonObject }
    private val cycleCases get() = doc["cycleCases"]!!.jsonArray.map { it.jsonObject }
    private val hydrationAverageCases get() = doc["hydrationAverageCases"]!!.jsonArray.map { it.jsonObject }
    private val config get() = doc["config"]!!.jsonObject

    /**
     * The vectors bake in the thresholds. If someone changes a constant without regenerating the
     * file, every downstream expectation is quietly wrong — so fail loudly and immediately.
     */
    @Test
    fun `the engine's constants match the contract the vectors were generated against`() {
        assertEquals(
            "minWeekDays in the vectors must match StreakEngine.WEEK_COMPLETE_DAYS",
            config["minWeekDays"]!!.jsonPrimitive.int,
            StreakEngine.WEEK_COMPLETE_DAYS,
        )
        assertEquals(
            "goalMl in the vectors must match StreakEngine.DEFAULT_GOAL_ML",
            config["goalMl"]!!.jsonPrimitive.int,
            StreakEngine.DEFAULT_GOAL_ML,
        )
    }

    @Test
    fun `the vector file actually loaded and has cases`() {
        // Guards the failure mode where the resource wiring breaks and every case silently vanishes,
        // leaving a green suite that asserts nothing.
        assertEquals(true, cases.size >= 16)
        assertEquals(true, cycleCases.size >= 8)
        assertEquals(true, hydrationAverageCases.size >= 4)
    }

    @Test
    fun `every vector holds`() {
        val goalMl = config["goalMl"]!!.jsonPrimitive.int
        val failures = mutableListOf<String>()

        for (case in cases) {
            val name = case["name"]!!.jsonPrimitive.content
            val today = LocalDate.parse(case["today"]!!.jsonPrimitive.content)
            val logs = case["logs"]!!.jsonArray.associate { element ->
                val o = element.jsonObject
                LocalDate.parse(o["date"]!!.jsonPrimitive.content) to o.toDailyLog()
            }
            val phDates = case["phDates"]!!.jsonArray
                .map { LocalDate.parse(it.jsonPrimitive.content) }
                .toSet()

            val state = StreakEngine.compute(logs, phDates, today, goalMl = goalMl)
            val expected = case["expected"]!!.jsonObject

            fun check(metric: String, actual: Int) {
                val want = expected[metric]!!.jsonPrimitive.int
                if (want != actual) failures += "[$name] $metric: expected $want but was $actual"
            }

            check("dailyActivity", state.dailyActivity)
            check("dailyHydration", state.dailyHydration)
            check("weeklyStreak", state.weeklyStreak)
            check("daysLoggedThisWeek", state.daysLoggedThisWeek)
            check("daysOnGoal", state.daysOnGoal)
        }

        // Report every mismatch at once — fixing them one failed run at a time is miserable.
        if (failures.isNotEmpty()) {
            throw AssertionError(
                "${failures.size} vector mismatch(es):\n" + failures.joinToString("\n") { "  $it" },
            )
        }
    }

    @Test
    fun `every cycle vector holds`() {
        val failures = mutableListOf<String>()

        for (case in cycleCases) {
            val name = case["name"]!!.jsonPrimitive.content
            val info = CycleEngine.getCyclePhase(
                lastPeriodDate = LocalDate.parse(case["lastPeriodDate"]!!.jsonPrimitive.content),
                cycleLength = case["cycleLength"]!!.jsonPrimitive.int,
                periodLength = case["periodLength"]!!.jsonPrimitive.int,
                target = LocalDate.parse(case["target"]!!.jsonPrimitive.content),
            )
            val expected = case["expected"]!!.jsonObject

            fun check(metric: String, actual: Any) {
                val want: Any = when (actual) {
                    is Int -> expected[metric]!!.jsonPrimitive.int
                    is Boolean -> expected[metric]!!.jsonPrimitive.boolean
                    else -> expected[metric]!!.jsonPrimitive.content
                }
                if (want != actual) failures += "[$name] $metric: expected $want but was $actual"
            }

            check("dayOfCycle", info.dayOfCycle)
            check("phase", info.phase.name)
            check("ovulationDay", info.ovulationDay)
            check("fertileWindowStart", info.fertileWindow.startDay)
            check("fertileWindowEnd", info.fertileWindow.endDay)
            check("inFertileWindow", info.dayOfCycle in info.fertileWindow)
            check("daysUntilNextPeriod", info.daysUntilNextPeriod)
        }

        if (failures.isNotEmpty()) {
            throw AssertionError(
                "${failures.size} cycle vector mismatch(es):\n" + failures.joinToString("\n") { "  $it" },
            )
        }
    }

    @Test
    fun `every hydration average vector holds`() {
        val failures = mutableListOf<String>()

        for (case in hydrationAverageCases) {
            val name = case["name"]!!.jsonPrimitive.content
            val today = LocalDate.parse(case["today"]!!.jsonPrimitive.content)
            val logs = case["logs"]!!.jsonArray.associate { element ->
                val o = element.jsonObject
                LocalDate.parse(o["date"]!!.jsonPrimitive.content) to o.toDailyLog()
            }
            val result = HydrationInsightLogic.compute(logs, today)
            val expected = case["expected"]!!.jsonObject

            fun check(metric: String, actual: Int?) {
                val want = expected[metric]!!.jsonPrimitive.intOrNull
                if (want != actual) failures += "[$name] $metric: expected $want but was $actual"
            }

            check("avgOnLoggedDaysMl", result.avgMlPerDay)
            check("deltaMlPerDay", result.deltaMlPerDay)
        }

        if (failures.isNotEmpty()) {
            throw AssertionError(
                "${failures.size} hydration vector mismatch(es):\n" + failures.joinToString("\n") { "  $it" },
            )
        }
    }

    /** A field the user left blank is absent from the JSON. `sleepMinutes: 0` is present, and real. */
    private fun JsonObject.toDailyLog(): DailyLog = DailyLog(
        mood = this["mood"]?.jsonPrimitive?.content?.let { id -> Mood.valueOf(id) },
        energy = this["energy"]?.jsonPrimitive?.content?.let { id -> EnergyLevel.valueOf(id) },
        symptoms = this["symptoms"]?.jsonArray?.map { it.jsonPrimitive.content }?.toSet().orEmpty(),
        sleepMinutes = this["sleepMinutes"]?.jsonPrimitive?.int,
        supplements = this["supplements"]?.jsonArray?.map { it.jsonPrimitive.content }?.toSet().orEmpty(),
        notes = this["notes"]?.jsonPrimitive?.content,
        waterMl = this["waterMl"]?.jsonPrimitive?.int ?: 0,
    )
}
