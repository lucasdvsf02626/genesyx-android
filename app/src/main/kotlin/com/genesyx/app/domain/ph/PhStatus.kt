package com.genesyx.app.domain.ph

import androidx.compose.ui.graphics.Color
import com.genesyx.app.ui.theme.PhAcidic
import com.genesyx.app.ui.theme.PhAlkaline
import com.genesyx.app.ui.theme.PhOptimal

/** Urine pH classification, ported from web `src/hooks/use-ph.ts`. */
enum class PhStatus(val label: String, val color: Color) {
    ACIDIC("Acidic", PhAcidic),
    OPTIMAL("Optimal", PhOptimal),
    ALKALINE("Alkaline", PhAlkaline);

    companion object {
        const val MIN = 4.5
        const val MAX = 9.0
        const val STEP = 0.1

        fun classify(value: Double): PhStatus = when {
            value < 6.0 -> ACIDIC
            value > 7.5 -> ALKALINE
            else -> OPTIMAL
        }
    }
}
