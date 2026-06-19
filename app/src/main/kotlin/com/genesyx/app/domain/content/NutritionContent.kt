package com.genesyx.app.domain.content

import androidx.compose.ui.graphics.Color
import com.genesyx.app.domain.model.Phase

/**
 * Nutrition-screen content, ported verbatim from the web `screens/Nutrition.tsx` (`PHASE_FOODS`,
 * `PHASE_DESCRIPTION`) + the supplement plan + `mockData.articles`. Note: the Nutrition screen uses
 * this richer per-phase data, NOT the shorter `phaseFoods` in CycleContent.
 */

/** Expandable focus-food row: colored dot + name + short blurb, expanding to a detailed blurb. */
data class PhaseFood(
    val name: String,
    val shortDesc: String,
    val expandedDesc: String,
    val accent: Color,
)

private val FoodPeriodAccent = Color(0xFFF48FB1)
private val FoodFollicularAccent = Color(0xFFA5D6A7)
private val FoodOvulatoryAccent = Color(0xFFCE93D8)
private val FoodLutealAccent = Color(0xFFB39DDB)

val nutritionPhaseFoods: Map<Phase, List<PhaseFood>> = mapOf(
    Phase.PERIOD to listOf(
        PhaseFood(
            "Iron-rich foods",
            "Replenish iron lost during bleeding.",
            "Red meat, lentils, and dark leafy greens help restore iron levels. Pair with vitamin C (like lemon juice) to boost absorption. Aim for 2–3 servings daily during your period.",
            FoodPeriodAccent,
        ),
        PhaseFood(
            "Anti-inflammatory foods",
            "Reduce cramping and inflammation.",
            "Omega-3 fatty acids found in salmon, chia seeds, and walnuts reduce prostaglandins that cause cramps. Turmeric in warm milk is a traditional remedy with scientific backing.",
            FoodPeriodAccent,
        ),
        PhaseFood(
            "Warming foods",
            "Support circulation and comfort.",
            "Ginger tea, warm soups, and cooked root vegetables are easier to digest and support circulation. Avoid cold, raw foods which can increase cramping for some people.",
            FoodPeriodAccent,
        ),
    ),
    Phase.FOLLICULAR to listOf(
        PhaseFood(
            "Fermented foods",
            "Support gut health and rising estrogen.",
            "Yoghurt, kefir, kimchi, and sauerkraut feed your gut microbiome, which plays a role in metabolising estrogen. A healthy gut supports hormonal balance throughout your cycle.",
            FoodFollicularAccent,
        ),
        PhaseFood(
            "Sprouted seeds",
            "Phytoestrogens to support follicle growth.",
            "Flaxseeds and pumpkin seeds contain lignans and zinc that support follicle development. Add to smoothies, yoghurt, or salads. Start seed cycling with flax + pumpkin in the first half of your cycle.",
            FoodFollicularAccent,
        ),
        PhaseFood(
            "Light proteins",
            "Fuel energy without heaviness.",
            "Eggs, tofu, and legumes provide amino acids for tissue repair and hormone production. Your digestion is stronger in the follicular phase, so it is a good time to try new foods.",
            FoodFollicularAccent,
        ),
    ),
    Phase.OVULATORY to listOf(
        PhaseFood(
            "Leafy greens",
            "Folate-rich foods to support egg quality.",
            "Spinach, kale, and rocket are rich in folate (B9), which supports egg quality and early fetal development if conception occurs. Aim for 2 generous handfuls per day during your fertile window.",
            FoodOvulatoryAccent,
        ),
        PhaseFood(
            "Complex carbs",
            "Steady energy and balanced blood sugar.",
            "Quinoa, sweet potato, and brown rice provide slow-release energy to support your peak activity levels. Avoid refined sugars which can cause energy crashes during your fertile window.",
            FoodOvulatoryAccent,
        ),
        PhaseFood(
            "Zinc-rich foods",
            "Support ovulation and immune function.",
            "Pumpkin seeds, shellfish, and beef liver are excellent zinc sources. Zinc is essential for the LH surge that triggers ovulation. Low zinc is linked to irregular ovulation.",
            FoodOvulatoryAccent,
        ),
        PhaseFood(
            "Antioxidant foods",
            "Protect egg quality from oxidative stress.",
            "Berries, colourful peppers, and tomatoes are rich in vitamins C and E. Antioxidants neutralise free radicals that can damage eggs. Include a rainbow of colours in each meal.",
            FoodOvulatoryAccent,
        ),
    ),
    Phase.LUTEAL to listOf(
        PhaseFood(
            "Magnesium-rich foods",
            "Ease PMS symptoms and support sleep.",
            "Dark chocolate (70%+), almonds, spinach, and pumpkin seeds are high in magnesium. Studies show magnesium supplementation reduces PMS severity including mood changes, bloating, and cramps.",
            FoodLutealAccent,
        ),
        PhaseFood(
            "B6 foods",
            "Support progesterone and reduce mood swings.",
            "Salmon, chicken, bananas, and sunflower seeds are rich in vitamin B6, which supports progesterone production and serotonin synthesis. Low B6 is strongly associated with PMS.",
            FoodLutealAccent,
        ),
        PhaseFood(
            "Fibre-rich foods",
            "Support estrogen clearance.",
            "As progesterone rises, your gut slows down. Oats, flaxseeds, and vegetables support bowel regularity and help clear excess estrogen from the body, reducing PMS bloating.",
            FoodLutealAccent,
        ),
        PhaseFood(
            "Complex carbs",
            "Reduce cravings and stabilise mood.",
            "Serotonin dips in the luteal phase, causing carb cravings. Complex carbs like oats, lentils, and whole grain bread boost serotonin naturally without the crash from refined sugar.",
            FoodLutealAccent,
        ),
    ),
)

val nutritionPhaseDescription: Map<Phase, String> = mapOf(
    Phase.PERIOD to "Foods to restore and replenish during your cycle.",
    Phase.FOLLICULAR to "Foods to support rising energy and hormone balance.",
    Phase.OVULATORY to "Foods chosen to gently support your body through this week of your cycle.",
    Phase.LUTEAL to "Foods to ease PMS and support your winding-down phase.",
)

/** Supplement-plan item shown as the F/O/D/Z stack + "Review Plan" dialog. */
data class SupplementPlanItem(val initial: String, val name: String, val rationale: String)

val supplementPlan = listOf(
    SupplementPlanItem("F", "Folate (400–800 mcg)", "Supports egg quality and early cell development."),
    SupplementPlanItem("O", "Omega-3 (DHA/EPA)", "Hormone balance and reduced inflammation."),
    SupplementPlanItem("D", "Vitamin D (600–1000 IU)", "Supports ovulation and overall wellbeing."),
    SupplementPlanItem("Z", "Zinc (8–11 mg)", "Supports the LH surge that triggers ovulation."),
)

/** Learn-more article tiles (title + read time), from `mockData.articles`. */
data class Article(val title: String, val read: String)

val nutritionArticles = listOf(
    Article("Eating for your luteal phase", "4 min read"),
    Article("How hydration shapes fertility", "3 min read"),
    Article("A gentle guide to supplements", "6 min read"),
)
