package com.genesyx.app.data.local.entity

/** Local sync state of a pH row against Supabase `ph_readings`. */
enum class PhSyncStatus {
    /** Matches the server (or pulled from it). */
    SYNCED,

    /** Created/edited locally, not yet pushed. */
    PENDING_UPSERT,

    /** Soft-deleted locally (deletedAt set), tombstone not yet pushed. */
    PENDING_DELETE,
}
