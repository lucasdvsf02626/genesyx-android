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
    val notes: String? = null,
    val waterMl: Int = 0,
)

/**
 * The single definition of a "meaningful log": any tracked field counts — water, mood, energy, a
 * symptom, sleep, supplements or a note. Both the streak engine and the weekly summary count days
 * through this one predicate, so they can never disagree about which days she logged.
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
        !notes.isNullOrBlank()
