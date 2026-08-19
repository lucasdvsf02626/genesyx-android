package com.genesyx.app.domain.content

/**
 * The NHS / EFSA / PubMed set iOS ships in `medical_sources.json`. Compile-time so a citation
 * cannot fail to load. [reviewed] is blank — those files have no review dates; pH [Citation]s
 * that do carry dates live on [com.genesyx.app.domain.ph.PhCopy.SOURCES].
 */
object MedicalSources {
    val all: List<Citation> = listOf(
        source("nhs-water", "Water, drinks and hydration", "NHS (UK)",
            "https://www.nhs.uk/live-well/eat-well/food-guidelines-and-food-labels/water-drinks-nutrition/"),
        source("valtin-2002", "\"Drink at least eight glasses of water a day.\" Really? (Valtin, 2002)",
            "American Journal of Physiology / PubMed", "https://pubmed.ncbi.nlm.nih.gov/12376390/"),
        source("vaginal-ph", "Bacterial vaginosis", "NHS (UK)",
            "https://www.nhs.uk/conditions/bacterial-vaginosis/"),
        source("statpearls-vaginitis", "Vaginitis (StatPearls)", "NCBI / National Library of Medicine",
            "https://www.ncbi.nlm.nih.gov/books/NBK470302/"),
        source("armstrong-2012", "Mild dehydration affects mood in healthy young women (Armstrong et al., 2012)",
            "Journal of Nutrition / PubMed", "https://pubmed.ncbi.nlm.nih.gov/22190027/"),
        source("efsa-water", "Scientific Opinion on Dietary Reference Values for water",
            "European Food Safety Authority (EFSA)", "https://www.efsa.europa.eu/en/efsajournal/pub/1459"),
        source("nhs-vitamins", "Vitamins and minerals", "NHS (UK)",
            "https://www.nhs.uk/conditions/vitamins-and-minerals/"),
        source("nhs-eatwell", "The Eatwell Guide", "NHS (UK)",
            "https://www.nhs.uk/live-well/eat-well/food-guidelines-and-food-labels/the-eatwell-guide/"),
        source("nhs-periods", "Periods and the menstrual cycle", "NHS (UK)",
            "https://www.nhs.uk/conditions/periods/"),
        source("nhs-iron", "Iron (vitamins and minerals)", "NHS (UK)",
            "https://www.nhs.uk/conditions/vitamins-and-minerals/iron/"),
        source("nhs-preconception", "Planning your pregnancy — folic acid and preconception", "NHS (UK)",
            "https://www.nhs.uk/pregnancy/trying-for-a-baby/planning-your-pregnancy/"),
        source("nhs-vitamin-d", "Vitamin D", "NHS (UK)",
            "https://www.nhs.uk/conditions/vitamins-and-minerals/vitamin-d/"),
        source("nhs-vitamin-b", "B vitamins and folic acid", "NHS (UK)",
            "https://www.nhs.uk/conditions/vitamins-and-minerals/vitamin-b/"),
        source("nhs-conception", "Trying to get pregnant", "NHS (UK)",
            "https://www.nhs.uk/pregnancy/trying-for-a-baby/trying-to-get-pregnant/"),
        source("nhs-infertility", "Infertility", "NHS (UK)",
            "https://www.nhs.uk/conditions/infertility/"),
        source("nhs-sleep", "Sleep and tiredness", "NHS (UK)",
            "https://www.nhs.uk/live-well/sleep-and-tiredness/how-to-get-to-sleep/"),
        source("nhs-stress", "Get help with stress", "NHS (UK)",
            "https://www.nhs.uk/mental-health/feelings-symptoms-behaviours/feelings-and-symptoms/stress/"),
        source("wilcox-1995",
            "Timing of intercourse in relation to ovulation — effects on conception and the sex of the baby (Wilcox et al., 1995)",
            "New England Journal of Medicine / PubMed", "https://pubmed.ncbi.nlm.nih.gov/7477165/"),
    )

    val byId: Map<String, Citation> = all.associateBy { it.id }

    fun require(id: String): Citation = requireNotNull(byId[id]) { "unknown medical source: $id" }

    private fun source(id: String, title: String, publisher: String, url: String) =
        Citation(id = id, title = title, publisher = publisher, reviewed = "", url = url)
}
