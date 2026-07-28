package com.genesyx.app.domain.hydration

/**
 * How the customer wants water amounts shown. A display preference only — storage, sync, goals and
 * the streak engine all stay in millilitres, so switching units never changes any stored number.
 * One cup is [HydrationFormat.CUP_ML] (250ml, the metric cup).
 */
enum class HydrationUnit { ML, CUPS }
