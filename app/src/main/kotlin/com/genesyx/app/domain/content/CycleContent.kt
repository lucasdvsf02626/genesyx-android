package com.genesyx.app.domain.content

import com.genesyx.app.domain.model.Phase

/** Phase copy + foods, ported verbatim from web `cycle.ts` (see docs/DATA_LAYER.md Part C). */

data class FocusCopy(val title: String, val body: String)

data class PhaseHeroCopy(
    val hero: String,
    val sub: String,
    val tags: List<String>,
    val focus: FocusCopy,
)

data class FocusFood(val title: String, val desc: String)

val phaseLabel: Map<Phase, String> = mapOf(
    Phase.PERIOD to "Period",
    Phase.FOLLICULAR to "Follicular Phase",
    Phase.OVULATORY to "Ovulatory Phase",
    Phase.LUTEAL to "Luteal Phase",
)

val phaseHeroCopy: Map<Phase, PhaseHeroCopy> = mapOf(
    Phase.PERIOD to PhaseHeroCopy(
        hero = "Rest and replenish your body",
        sub = "Energy is naturally lower — choose iron-rich, warming meals.",
        tags = listOf("Low estrogen", "Restore iron"),
        focus = FocusCopy("Add a warm iron-rich meal", "Lentils, beef, or dark greens help replenish what's lost."),
    ),
    Phase.FOLLICULAR to PhaseHeroCopy(
        hero = "Building energy for ovulation",
        sub = "Estrogen is rising. Focus on fresh, nutrient-dense foods.",
        tags = listOf("Estrogen rising", "Building energy"),
        focus = FocusCopy("Add 2 cups of leafy greens", "Folate-forward foods support egg quality."),
    ),
    Phase.OVULATORY to PhaseHeroCopy(
        hero = "High chance of conception today",
        sub = "Ovulation expected in 1–2 days. Stay hydrated and rested.",
        tags = listOf("High estrogen", "Peak energy"),
        focus = FocusCopy("Hydrate and prioritise protein", "Eggs, salmon, and avocado support hormone balance."),
    ),
    Phase.LUTEAL to PhaseHeroCopy(
        hero = "Slow down and nourish",
        sub = "Progesterone rises. Choose magnesium-rich foods to ease symptoms.",
        tags = listOf("Progesterone rising", "Lower energy"),
        focus = FocusCopy("Try a magnesium-rich snack", "Pumpkin seeds, dark chocolate, or bananas help mood + sleep."),
    ),
)

val phaseFoods: Map<Phase, List<FocusFood>> = mapOf(
    Phase.PERIOD to listOf(
        FocusFood("Lentils & beans", "Plant iron to replenish what's lost during menstruation."),
        FocusFood("Dark leafy greens", "Spinach and kale pair iron with folate for steady energy."),
        FocusFood("Bone broth", "Warming, mineral-rich, gentle on a tender gut."),
        FocusFood("Dark chocolate", "Magnesium to soften cramps and lift mood."),
    ),
    Phase.FOLLICULAR to listOf(
        FocusFood("Sprouted grains", "Steady carbs for rising estrogen and morning energy."),
        FocusFood("Fermented foods", "Kimchi or kefir support estrogen metabolism."),
        FocusFood("Citrus & berries", "Vitamin C supports collagen and egg quality."),
        FocusFood("Pumpkin seeds", "Zinc to fuel the building phase of your cycle."),
    ),
    Phase.OVULATORY to listOf(
        FocusFood("Wild salmon", "Omega-3s support hormone balance at ovulation."),
        FocusFood("Avocado", "Healthy fats help your body use estrogen well."),
        FocusFood("Eggs", "Choline and B12 — a complete fertility breakfast."),
        FocusFood("Leafy greens", "Folate supports cell division and conception."),
    ),
    Phase.LUTEAL to listOf(
        FocusFood("Sweet potato", "Slow carbs to steady progesterone-driven cravings."),
        FocusFood("Pumpkin seeds", "Magnesium to ease PMS and improve sleep."),
        FocusFood("Bananas", "B6 to lift mood as the luteal phase winds down."),
        FocusFood("Turkey", "Tryptophan helps with rest and calm."),
    ),
)
