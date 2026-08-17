package com.genesyx.app.domain.content

import com.genesyx.app.domain.model.Phase

/**
 * The food groups she can tick off for a day.
 *
 * The Eatwell Guide's five, with fruit and vegetables split apart — the Guide counts them as one
 * group, but a day with fruit and no vegetables is precisely the day worth being able to record.
 *
 * Raw values are the shared `daily_logs.food_groups` tokens. Do not rename them.
 *
 * A fixed vocabulary rather than a food database: naming a group states nothing about what any of
 * it *does*, so this screen carries no claim for a medical reviewer.
 */
enum class FoodGroup(val raw: String, val label: String, val examples: String) {
    VEGETABLES("vegetables", "Vegetables", "Salad, greens, peppers, carrots"),
    FRUIT("fruit", "Fruit", "Berries, apples, bananas, citrus"),
    STARCHY_CARBS("starchyCarbs", "Starchy carbs", "Potatoes, bread, rice, pasta, oats"),
    PROTEIN("protein", "Protein", "Beans, pulses, fish, eggs, meat, tofu"),
    DAIRY("dairy", "Dairy & alternatives", "Milk, yoghurt, cheese, fortified alternatives"),
    OILS_AND_FATS("oilsAndFats", "Oils & fats", "Olive oil, nuts, seeds, avocado"),
    ;

    companion object {
        fun fromRaw(raw: String): FoodGroup? = entries.find { it.raw == raw }

        /** How many of the six named groups appear in [logged]. Unknown tokens do not count. */
        fun knownCount(logged: Set<String>): Int = entries.count { it.raw in logged }
    }
}

/**
 * Reader-facing copy for the food log. In the domain so the content guards can see it.
 *
 * A food log is the easiest surface in a fertility app to turn into a scoreboard, and a
 * scoreboard is the thing she least needs from it.
 */
object FoodLogCopy {
    const val title = "What you ate today"
    const val footnote =
        "A record, not a target. Nothing here is scored, and a blank day costs you nothing."
    const val whatCountsLabel = "What counts as what?"

    fun summary(logged: Int, total: Int): String =
        if (logged == 0) {
            "Tap a group when you have eaten something from it."
        } else {
            "$logged of $total groups so far today."
        }

    /**
     * Names the groups the focus-foods card above already leans on. A statement about the
     * screen, not advice.
     */
    fun phaseLine(groups: List<FoodGroup>): String? {
        if (groups.isEmpty()) return null
        return "Your focus foods this phase lean on ${sentenceList(groups.map { it.label.lowercase() })}."
    }

    fun sentenceList(items: List<String>): String {
        val last = items.lastOrNull() ?: return ""
        if (items.size == 1) return last
        return items.dropLast(1).joinToString(", ") + " and " + last
    }

    /** Everything this card can render, for the banned-phrase guard. */
    val allStrings: List<String>
        get() = buildList {
            add(title)
            add(footnote)
            add(whatCountsLabel)
            FoodGroup.entries.forEach {
                add(it.label)
                add(it.examples)
            }
            val total = FoodGroup.entries.size
            (0..total).forEach { add(summary(it, total)) }
            Phase.entries.forEach { phase ->
                phaseLine(nutritionPhaseFoodGroups.getValue(phase))?.let(::add)
            }
        }
}

/**
 * Which groups the phase's focus foods already lean on — read off that copy, so the line
 * repeats guidance rather than adding any.
 */
val nutritionPhaseFoodGroups: Map<Phase, List<FoodGroup>> = mapOf(
    Phase.PERIOD to listOf(FoodGroup.PROTEIN, FoodGroup.VEGETABLES, FoodGroup.OILS_AND_FATS),
    Phase.FOLLICULAR to listOf(FoodGroup.DAIRY, FoodGroup.PROTEIN, FoodGroup.OILS_AND_FATS),
    Phase.OVULATORY to listOf(FoodGroup.VEGETABLES, FoodGroup.FRUIT, FoodGroup.PROTEIN, FoodGroup.STARCHY_CARBS),
    Phase.LUTEAL to listOf(FoodGroup.STARCHY_CARBS, FoodGroup.VEGETABLES, FoodGroup.PROTEIN, FoodGroup.OILS_AND_FATS),
)
