package com.genesyx.app.domain.ph

import androidx.compose.ui.graphics.Color
import com.genesyx.app.ui.theme.PhElevated
import com.genesyx.app.ui.theme.PhOptimal

/**
 * Vaginal-pH classification. A two-band model — [HEALTHY] (within the typical range) vs [ELEVATED]
 * (above it) — replacing the previous urine-pH acidic/optimal/alkaline scheme.
 *
 * ⚠️ PROVISIONAL — pending client sign-off. Figures follow the standard published healthy vaginal
 * pH range (~3.8–4.5, elevated >4.5). Genesyx is a wellness app, not a medical device; no
 * clinical/regulatory approval applies. The input range ([MIN]..[MAX]), the healthy band
 * ([HEALTHY_MIN]..[HEALTHY_MAX]) and the elevated threshold (> [HEALTHY_MAX]) are provisional until
 * that sign-off (see CHANGELOG "Unreleased").
 */
enum class PhStatus(val label: String, val color: Color) {
    HEALTHY("Healthy", PhOptimal),
    ELEVATED("Elevated", PhElevated);

    companion object {
        // PROVISIONAL — pending client sign-off (see class doc).
        const val MIN = 3.5
        const val MAX = 7.0
        const val STEP = 0.1

        /** Typical healthy vaginal-pH band (provisional). Readings above [HEALTHY_MAX] are ELEVATED. */
        const val HEALTHY_MIN = 3.8
        const val HEALTHY_MAX = 4.5

        fun classify(value: Double): PhStatus =
            if (value > HEALTHY_MAX) ELEVATED else HEALTHY
    }
}
