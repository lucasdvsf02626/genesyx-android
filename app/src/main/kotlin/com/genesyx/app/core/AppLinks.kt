package com.genesyx.app.core

/** External web links used from the app (hosted on the marketing site). */
object AppLinks {
    const val PRIVACY_POLICY_URL = "https://genesyx.co.uk/pages/privacy-policy"
    const val DELETE_ACCOUNT_URL = "https://genesyx.co.uk/pages/delete-account"
    const val SUPPORT_EMAIL = "info@genesyx.co.uk"

    /**
     * Used in the article Share sheet. Deliberately the site root, not `/blog/{slug}` — no per-article
     * page is confirmed to exist, and a shared 404 is worse than a shared homepage. Point this at the
     * article URL once the marketing site hosts them.
     */
    const val SITE_URL = "https://genesyx.co.uk"

    /**
     * Marketing science / evidence landing. Dedicated `/pages/science` is not confirmed to exist,
     * so this currently opens the site root — the in-app citations on the pH screen remain the
     * actual sources. Point this at the science page once the site hosts it.
     */
    const val SCIENCE_URL = SITE_URL

    /**
     * Shettles is a theory, not a proven method. The in-app article
     * [SHETTLES_ARTICLE_SLUG] is the canonical write-up (date-gated in Learn). This URL is the
     * external fallback when that article is not yet published.
     */
    const val SHETTLES_THEORY_URL = SITE_URL
    const val SHETTLES_ARTICLE_SLUG = "shettles-method-theory-vs-evidence"
}
