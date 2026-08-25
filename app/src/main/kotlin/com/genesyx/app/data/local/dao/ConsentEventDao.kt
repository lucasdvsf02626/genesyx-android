package com.genesyx.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.genesyx.app.data.local.entity.ConsentEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Append-only by construction: this DAO exposes [insert] and reads, and deliberately offers no
 * update or delete. Withdrawing consent adds a WITHDRAWN row; it never removes the GRANTED one.
 *
 * Account deletion still clears the table, but through `clearAllTables()` rather than anything
 * reachable from here — an erasure request is a different thing from a withdrawal.
 */
@Dao
interface ConsentEventDao {
    @Insert
    suspend fun insert(event: ConsentEventEntity)

    /**
     * The newest event, which is what decides whether collection is currently permitted.
     *
     * Ordered by rowid, not `recordedAt`: the timestamp is stored as ISO text, and
     * `LocalDateTime.toString()` drops the seconds when they are zero — so "…T14:31" sorts after
     * "…T14:30:05". Ties broke on a random UUID. rowid is the insertion order this trail actually
     * means, and getting it backwards would mean honouring a withdrawal she had already reversed.
     */
    @Query("SELECT * FROM consent_events WHERE userId = :userId ORDER BY rowid DESC LIMIT 1")
    fun observeLatest(userId: String): Flow<ConsentEventEntity?>

    /** Same decision, read once — used by the gate, which must not race a cold-start flow. */
    @Query("SELECT * FROM consent_events WHERE userId = :userId ORDER BY rowid DESC LIMIT 1")
    suspend fun latest(userId: String): ConsentEventEntity?

    /** The whole trail, oldest first. This is the audit evidence. */
    @Query("SELECT * FROM consent_events WHERE userId = :userId ORDER BY recordedAt ASC, id ASC")
    fun observeTrail(userId: String): Flow<List<ConsentEventEntity>>
}
