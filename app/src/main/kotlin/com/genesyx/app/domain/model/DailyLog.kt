package com.genesyx.app.domain.model

/** Mood options shown on Log Today (web ids: great/good/ok/low). */
enum class Mood(val id: String, val label: String) {
    GREAT("great", "Great"),
    GOOD("good", "Good"),
    OKAY("ok", "Okay"),
    LOW("low", "Low"),
}

/** Energy level segmented control (web: low/normal/high). */
enum class EnergyLevel(val id: String) { LOW("low"), NORMAL("normal"), HIGH("high") }

/** A full daily log entry. Mirrors `daily_logs` (docs/DATA_LAYER.md). */
data class DailyLog(
    val mood: Mood? = null,
    val energy: EnergyLevel? = null,
    val symptoms: Set<String> = emptySet(),
    val sleepMinutes: Int? = null,
    val supplements: Set<String> = emptySet(),
    /**
     * Food groups eaten that day, as free tokens rather than an enum so a group added on iOS (the
     * only client that can currently log them) decodes here instead of throwing. Mirrors
     * `daily_logs.food_groups text[]`.
     */
    val foodGroups: Set<String> = emptySet(),
    val notes: String? = null,
    val waterMl: Int = 0,
    /**
     * Private intimacy record — null = not recorded, true = recorded (v7). Never surfaced to any
     * partner feature. Deliberately NOT part of [isMeaningful]: the qualifying-action set is pinned
     * by the cross-platform tracking contract (tracking_test_vectors.json), and this field is
     * always entered alongside fields that already qualify the day.
     */
    val sexualActivity: Boolean? = null,
)

/**
 * The single definition of a "meaningful log": any tracked field counts — water, mood, energy, a
 * symptom, sleep, supplements, food groups or a note. Both the streak engine and the weekly summary
 * count days through this one predicate, so they can never disagree about which days she logged.
 *
 * The qualifying set is a cross-platform contract: the iOS `TrackingEngine.isMeaningfulLog` carries
 * the same terms, so identical backend rows must produce identical streaks on both phones. Adding a
 * term on one client alone silently gives two different numbers for the same data, with nothing
 * anywhere to report it. `foodGroups` joined the set on both clients together (H4).
 *
 * Sleep is `!= null`, deliberately, not `> 0`: null means "not entered", so an explicitly logged
 * zero is a real record. Someone logging an all-nighter *is* logging, and that is exactly the day
 * she most deserves credit for tracking — `> 0` would silently discount it.
 */
fun DailyLog.isMeaningful(): Boolean =
    waterMl > 0 ||
        mood != null ||
        energy != null ||
        symptoms.isNotEmpty() ||
        sleepMinutes != null ||
        supplements.isNotEmpty() ||
        foodGroups.isNotEmpty() ||
        !notes.isNullOrBlank()
