package com.genesyx.app.domain.content

/**
 * Maps a Learn slug to [MedicalSources] ids. Port of iOS `LearnSourceMap`. Behavioural articles
 * have no entry and rely on the medical disclaimer alone.
 */
object LearnSourceMap {
    val bySlug: Map<String, List<String>> = mapOf(
        "hydration-basics" to listOf("nhs-water", "valtin-2002", "efsa-water"),
        "eating-with-your-cycle" to listOf("nhs-periods", "nhs-iron", "nhs-eatwell"),
        "gentle-guide-supplements" to listOf("nhs-preconception", "nhs-vitamin-b", "nhs-vitamin-d"),
        "guide-vaginal-ph-tracker" to listOf("vaginal-ph", "statpearls-vaginitis"),
        "guide-how-to-log-ph" to listOf("vaginal-ph", "statpearls-vaginitis"),
        "guide-nutrition-focus" to listOf("nhs-periods", "nhs-eatwell"),
        "guide-how-hydration-works" to listOf("nhs-water", "armstrong-2012"),
        "guide-track-ph-in-nutrition" to listOf("vaginal-ph", "statpearls-vaginitis"),
        "guide-cycle-and-phases" to listOf("nhs-periods", "nhs-conception"),
        "guide-understanding-vaginal-ph" to listOf("vaginal-ph", "statpearls-vaginitis"),
        "fertile-window" to listOf("nhs-conception", "nhs-periods"),
        "vaginal-ph-explained" to listOf("vaginal-ph", "statpearls-vaginitis"),
        "nutrition-before-conception" to listOf("nhs-preconception", "nhs-vitamin-b", "nhs-vitamin-d", "nhs-eatwell"),
        "cervical-mucus" to listOf("nhs-conception", "nhs-periods"),
        "hydration-and-reproductive-health" to listOf("nhs-water", "efsa-water", "armstrong-2012"),
        "timing-sex-when-ttc" to listOf("nhs-conception"),
        "sleep-stress-and-your-cycle" to listOf("nhs-sleep", "nhs-stress"),
        "understanding-ovulation-tests" to listOf("nhs-conception", "nhs-periods"),
        "supporting-sperm-health" to listOf("nhs-infertility", "nhs-preconception"),
        "fertility-supplements-explained" to listOf("nhs-preconception", "nhs-vitamin-b", "nhs-vitamin-d", "nhs-vitamins"),
        "when-to-ask-for-support" to listOf("nhs-infertility", "nhs-conception"),
        "shettles-method" to listOf("wilcox-1995", "nhs-conception"),
        "shettles-method-theory-vs-evidence" to listOf("wilcox-1995", "nhs-conception"),
    )

    fun citationsFor(slug: String): List<Citation> {
        val canonical = articleBySlug(slug)?.slug ?: slug
        val ids = bySlug[canonical] ?: bySlug[slug] ?: return emptyList()
        return ids.map(MedicalSources::require)
    }
}
