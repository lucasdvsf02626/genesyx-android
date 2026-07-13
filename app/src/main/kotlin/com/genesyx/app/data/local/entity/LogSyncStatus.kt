package com.genesyx.app.data.local.entity

/**
 * Local sync state of a daily-log row against Supabase `daily_logs`.
 *
 * There is no PENDING_DELETE twin of [PhSyncStatus]'s: a daily log is never deleted, only edited to
 * blank. If a delete path is ever added, it needs a tombstone like pH has.
 */
enum class LogSyncStatus {
    /** Matches the server (or was pulled from it). */
    SYNCED,

    /** Written/edited locally, not yet pushed. A pull must not overwrite this row. */
    PENDING_UPSERT,
}
