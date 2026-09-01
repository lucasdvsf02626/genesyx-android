package com.genesyx.app.data.remote

import io.github.jan.supabase.exceptions.RestException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * The consent push is a plain INSERT (the table has no UPDATE policy, so upsert's conflict
 * branch is refused rather than merged). A replayed push collides on the device-minted id, and
 * that collision — and ONLY that — reads as success. Everything else must stay a failure:
 * hiding a schema or auth error is exactly how the code-22 defect stayed invisible.
 */
class ConsentInsertReplayTest {

    @Test
    fun `a duplicate-key rejection is a replay, which is success`() {
        // PostgREST surfaces Postgres unique_violation as HTTP 409 / code 23505.
        assertTrue(isDuplicateKey(RestException("23505", "duplicate key value violates unique constraint", 409, "Conflict")))
        assertTrue(isDuplicateKey(RestException("conflict", null, 409, "Conflict")))
    }

    @Test
    fun `a schema mismatch is NOT a replay`() {
        // The exact failure code 22 shipped with — it must surface, never be swallowed.
        assertFalse(
            isDuplicateKey(
                RestException("PGRST204", "Could not find the 'recorded_at' column of 'consent_events' in the schema cache", 400, "Bad Request"),
            ),
        )
    }

    @Test
    fun `auth, RLS and transport failures are NOT replays`() {
        assertFalse(isDuplicateKey(RestException("42501", "new row violates row-level security policy", 403, "Forbidden")))
        assertFalse(isDuplicateKey(RestException("PGRST301", "JWT expired", 401, "Unauthorized")))
        assertFalse(isDuplicateKey(IOException("timeout")))
    }
}
