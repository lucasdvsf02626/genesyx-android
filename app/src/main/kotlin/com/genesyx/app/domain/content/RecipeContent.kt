package com.genesyx.app.domain.content

import androidx.annotation.DrawableRes
import com.genesyx.app.R
import com.genesyx.app.domain.model.Nutrient
import com.genesyx.app.domain.model.Phase

/**
 * Recipe cards on the Nutrition tab. Ported from iOS `RecipeContent.swift`.
 *
 * Each recipe names a focus food that [nutritionPhaseFoods] already recommends for that phase.
 * That foreign key is why these cards carry no citation of their own — they cook a reviewed
 * food, they do not recommend a new one. `RecipeContentTest` fails if the name drifts.
 */
data class Recipe(
    val id: String,
    val title: String,
    /** One-line hook shown under the title. */
    val subtitle: String,
    /** The phase this recipe best suits; null = good any time (always shown). */
    val phase: Phase? = null,
    val prepMinutes: Int? = null,
    val serves: Int? = null,
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val nutrients: List<Nutrient> = emptyList(),
    /** Focus food this meal cooks. Must match a [PhaseFood.name] in the same phase. */
    val usesFocusFood: String? = null,
    /** Groups this meal covers, as `FoodGroup` raw values (vegetables, fruit, …). */
    val groups: List<String> = emptyList(),
    @DrawableRes val imageRes: Int? = null,
)

object RecipeCopy {
    const val title = "Something to cook"
    const val eyebrow = "Recipes"
    const val comingSoon =
        "Cycle-friendly recipes are coming soon. They'll appear here the moment they're added."
    const val ingredientsHeading = "You'll need"
    const val methodHeading = "Method"
    const val footnote = "Recipes are a starting point — swap anything that does not suit you."

    fun usesLine(focusFood: String): String = "Uses your focus food: $focusFood"

    fun meta(minutes: Int?, serves: Int?): String = buildList {
        minutes?.let { add("$it min") }
        serves?.let { add("serves $it") }
    }.joinToString(" · ")

    /** Additive log of the groups this recipe already covers. */
    fun logGroupsAction(groups: List<String>): String {
        val labels = groups.mapNotNull { FoodGroup.fromRaw(it)?.label?.lowercase() }
        return if (labels.size == 1) "Log ${labels[0]}"
        else "Log ${FoodLogCopy.sentenceList(labels)}"
    }
}

/**
 * Two recipes per cycle phase. `usesFocusFood` matches [nutritionPhaseFoods] exactly.
 */
val recipeContent: List<Recipe> = listOf(
    Recipe(
        id = "lentil-spinach-lemon-dal",
        title = "Lentil, spinach and lemon dal",
        subtitle = RecipeCopy.usesLine("Iron-rich foods"),
        phase = Phase.PERIOD,
        prepMinutes = 30,
        serves = 2,
        usesFocusFood = "Iron-rich foods",
        groups = listOf("protein", "vegetables", "oilsAndFats"),
        imageRes = R.drawable.recipe_lentil_spinach_lemon_dal,
        ingredients = listOf(
            "150g red lentils, rinsed",
            "1 onion, finely chopped",
            "2 garlic cloves, crushed",
            "1 tsp ground cumin and 1 tsp ground turmeric",
            "400ml vegetable stock",
            "100g spinach",
            "Juice of half a lemon",
            "1 tbsp olive oil",
        ),
        steps = listOf(
            "Soften the onion in the oil over a low heat for 5 minutes, then add the garlic and spices for one minute more.",
            "Add the lentils and stock. Simmer for 20 minutes, stirring now and then, until the lentils collapse.",
            "Stir the spinach through until it wilts.",
            "Take off the heat and add the lemon juice. Season to taste.",
        ),
    ),
    Recipe(
        id = "ginger-sweet-potato-soup",
        title = "Ginger and sweet potato soup",
        subtitle = RecipeCopy.usesLine("Warming foods"),
        phase = Phase.PERIOD,
        prepMinutes = 35,
        serves = 3,
        usesFocusFood = "Warming foods",
        groups = listOf("vegetables", "starchyCarbs", "oilsAndFats"),
        imageRes = R.drawable.recipe_ginger_sweet_potato_soup,
        ingredients = listOf(
            "2 sweet potatoes (about 500g), peeled and cubed",
            "1 thumb of fresh ginger, grated",
            "1 onion, chopped",
            "700ml vegetable stock",
            "1 tbsp olive oil",
            "Black pepper",
        ),
        steps = listOf(
            "Soften the onion in the oil for 5 minutes, then add the ginger for one minute.",
            "Add the sweet potato and stock. Bring to a simmer and cook for 20–25 minutes, until a knife slides through the potato easily.",
            "Blend until smooth, adding a splash of hot water if it is thicker than you want.",
            "Season with black pepper and serve hot.",
        ),
    ),
    Recipe(
        id = "kefir-berry-breakfast-bowl",
        title = "Kefir and berry breakfast bowl",
        subtitle = RecipeCopy.usesLine("Fermented foods"),
        phase = Phase.FOLLICULAR,
        prepMinutes = 5,
        serves = 1,
        usesFocusFood = "Fermented foods",
        groups = listOf("dairy", "fruit", "oilsAndFats"),
        imageRes = R.drawable.recipe_kefir_berry_breakfast_bowl,
        ingredients = listOf(
            "200ml kefir or live yoghurt",
            "80g mixed berries, fresh or frozen",
            "1 tbsp mixed seeds",
            "1 tsp honey, optional",
        ),
        steps = listOf(
            "Spoon the kefir into a bowl.",
            "Top with the berries and seeds.",
            "Add honey if you want it sweeter. If the berries are frozen, leave it to stand for five minutes.",
        ),
    ),
    Recipe(
        id = "sprouted-seed-tofu-traybake",
        title = "Sprouted seed and tofu traybake",
        subtitle = RecipeCopy.usesLine("Sprouted seeds"),
        phase = Phase.FOLLICULAR,
        prepMinutes = 30,
        serves = 2,
        usesFocusFood = "Sprouted seeds",
        groups = listOf("protein", "vegetables", "oilsAndFats"),
        imageRes = R.drawable.recipe_sprouted_seed_tofu_traybake,
        ingredients = listOf(
            "280g firm tofu, pressed and cubed",
            "2 tbsp pumpkin seeds",
            "1 tbsp ground flaxseed",
            "1 red pepper and 1 courgette, chopped",
            "1 tbsp olive oil",
            "1 tsp smoked paprika",
        ),
        steps = listOf(
            "Heat the oven to 200°C (180°C fan).",
            "Toss the tofu and vegetables with the oil and paprika, spread on a tray and roast for 20 minutes.",
            "Scatter over the pumpkin seeds and roast for a further 5 minutes.",
            "Sprinkle the ground flaxseed over just before serving.",
        ),
    ),
    Recipe(
        id = "big-green-quinoa-salad",
        title = "Big green salad with quinoa",
        subtitle = RecipeCopy.usesLine("Leafy greens"),
        phase = Phase.OVULATORY,
        prepMinutes = 25,
        serves = 2,
        usesFocusFood = "Leafy greens",
        groups = listOf("vegetables", "starchyCarbs", "oilsAndFats"),
        imageRes = R.drawable.recipe_big_green_quinoa_salad,
        ingredients = listOf(
            "100g quinoa",
            "2 large handfuls of spinach and rocket",
            "1 avocado, sliced",
            "1 tbsp pumpkin seeds",
            "Juice of half a lemon and 1 tbsp olive oil",
        ),
        steps = listOf(
            "Cook the quinoa in plenty of water for 15 minutes, then drain and let it cool a little.",
            "Whisk the lemon juice and oil together with a pinch of salt.",
            "Toss the leaves, quinoa and avocado with the dressing.",
            "Finish with the pumpkin seeds.",
        ),
    ),
    Recipe(
        id = "rainbow-pepper-bean-bowl",
        title = "Rainbow pepper and bean bowl",
        subtitle = RecipeCopy.usesLine("Antioxidant foods"),
        phase = Phase.OVULATORY,
        prepMinutes = 20,
        serves = 2,
        usesFocusFood = "Antioxidant foods",
        groups = listOf("vegetables", "protein", "starchyCarbs", "oilsAndFats"),
        imageRes = R.drawable.recipe_rainbow_pepper_bean_bowl,
        ingredients = listOf(
            "2 peppers of different colours, sliced",
            "1 tin (400g) black beans, drained",
            "200g cherry tomatoes, halved",
            "150g brown rice, cooked",
            "1 tbsp olive oil, juice of half a lime",
        ),
        steps = listOf(
            "Fry the peppers in the oil over a high heat for 6–8 minutes, until they take a little colour.",
            "Add the beans and tomatoes and warm through for 3 minutes.",
            "Spoon over the rice and finish with the lime juice.",
        ),
    ),
    Recipe(
        id = "dark-chocolate-almond-oat-bars",
        title = "Dark chocolate and almond oat bars",
        subtitle = RecipeCopy.usesLine("Magnesium-rich foods"),
        phase = Phase.LUTEAL,
        prepMinutes = 40,
        serves = 8,
        usesFocusFood = "Magnesium-rich foods",
        groups = listOf("starchyCarbs", "fruit", "oilsAndFats"),
        imageRes = R.drawable.recipe_dark_chocolate_almond_oat_bars,
        ingredients = listOf(
            "200g rolled oats",
            "100g almonds, roughly chopped",
            "60g dark chocolate (70%), chopped",
            "2 ripe bananas, mashed",
            "3 tbsp olive or rapeseed oil",
            "2 tbsp honey",
        ),
        steps = listOf(
            "Heat the oven to 180°C (160°C fan) and line a small tin.",
            "Mix everything together in a bowl until the oats are evenly coated.",
            "Press firmly into the tin and bake for 25 minutes, until golden at the edges.",
            "Cool completely in the tin before cutting, or the bars will crumble.",
        ),
    ),
    Recipe(
        id = "salmon-oats-greens-traybake",
        title = "Salmon, oats and greens traybake",
        subtitle = RecipeCopy.usesLine("B6 foods"),
        phase = Phase.LUTEAL,
        prepMinutes = 30,
        serves = 2,
        usesFocusFood = "B6 foods",
        groups = listOf("protein", "vegetables", "starchyCarbs", "oilsAndFats"),
        imageRes = R.drawable.recipe_salmon_oats_greens_traybake,
        ingredients = listOf(
            "2 salmon fillets",
            "2 tbsp oats and 1 tbsp sunflower seeds, for the crust",
            "200g tenderstem broccoli",
            "1 tbsp olive oil",
            "Half a lemon",
        ),
        steps = listOf(
            "Heat the oven to 200°C (180°C fan).",
            "Mix the oats and sunflower seeds with half the oil and press onto the salmon fillets.",
            "Toss the broccoli in the rest of the oil and spread on a tray with the salmon.",
            "Bake for 15–18 minutes, until the salmon flakes with a fork. Squeeze the lemon over to serve.",
        ),
    ),
)

/**
 * Recipes for a given cycle phase, plus any untagged (any-time) ones.
 * Before a cycle is set up, the whole library is shown so the section is not empty.
 */
fun recipesFor(phase: Phase?): List<Recipe> =
    if (phase == null) recipeContent
    else recipeContent.filter { it.phase == null || it.phase == phase }
