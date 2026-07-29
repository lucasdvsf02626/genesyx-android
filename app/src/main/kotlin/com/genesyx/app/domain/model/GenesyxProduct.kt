package com.genesyx.app.domain.model

/** One product of the Genesyx range, read from the shared `genesyx_products` catalogue. The range
 *  currently has zero SKUs — the UI renders a "coming soon" state when the catalogue is empty. */
data class GenesyxProduct(
    val id: String,
    val name: String,
    /** Dose label as printed on the product; free text, never parsed. */
    val dose: String? = null,
)
