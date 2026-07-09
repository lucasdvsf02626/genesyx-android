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
}
