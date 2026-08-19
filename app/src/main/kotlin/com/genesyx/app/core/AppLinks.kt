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
     * so this currently equals [SITE_URL]. [isConfiguredWebPage] is false until it points at a
     * real page — do not open the homepage and label it science.
     */
    const val SCIENCE_URL = SITE_URL

    /**
     * External Shettles write-up. Equals [SITE_URL] until a real page exists. The in-app article
     * ([SHETTLES_ARTICLE_SLUG], iOS alias [SHETTLES_IOS_SLUG]) is the canonical source.
     */
    const val SHETTLES_THEORY_URL = SITE_URL
    const val SHETTLES_ARTICLE_SLUG = "shettles-method-theory-vs-evidence"
    /** iOS Learn slug — `articleBySlug` resolves it to [SHETTLES_ARTICLE_SLUG]. */
    const val SHETTLES_IOS_SLUG = "shettles-method"

    /** True only when [url] is a distinct page, not the site root used as a placeholder. */
    fun isConfiguredWebPage(url: String): Boolean = url.isNotBlank() && url != SITE_URL
}
