package com.genesyx.app.data.local.entity

/** Local sync state of a user-supplement row against Supabase `user_supplements`. */
enum class SupplementSyncStatus {
    /** Matches the server (or pulled from it). */
    SYNCED,

    /** Created/edited locally, not yet pushed. */
    PENDING_UPSERT,

    /** Soft-deleted locally (deletedAt set), tombstone not yet pushed. */
    PENDING_DELETE,
}
