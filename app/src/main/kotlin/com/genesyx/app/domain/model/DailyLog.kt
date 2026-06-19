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
