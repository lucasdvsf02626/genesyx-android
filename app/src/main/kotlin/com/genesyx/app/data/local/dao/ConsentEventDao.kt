package com.genesyx.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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
     * Adopt server rows pulled by the consent sync. IGNORE on the id, not update: a replayed pull
     * can never rewrite an event, so the trail stays append-only even through a merge.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(events: List<ConsentEventEntity>)

    /**
     * The whole trail in insertion order, for the merge and for deriving the newest event in
     * Kotlin (compare real timestamps, insertion order as the tiebreak — see the rowid note below).
     */
    @Query("SELECT * FROM consent_events WHERE userId = :userId ORDER BY rowid ASC")
    suspend fun trailByInsertion(userId: String): List<ConsentEventEntity>

    /** [trailByInsertion] as a flow — drives the live consent state. */
    @Query("SELECT * FROM consent_events WHERE userId = :userId ORDER BY rowid ASC")
    fun observeTrailByInsertion(userId: String): Flow<List<ConsentEventEntity>>

    /**
     * The newest locally-inserted event, by rowid rather than `recordedAt`: the timestamp is
     * stored as ISO text, and `LocalDateTime.toString()` drops the seconds when they are zero — so
     * "…T14:31" sorts after "…T14:30:05". For a purely local trail rowid IS chronology; once the
     * server merge is involved, ConsentRepository derives the newest event in Kotlin instead
     * (real timestamp comparison over [trailByInsertion]). Used for existence checks.
     */
    @Query("SELECT * FROM consent_events WHERE userId = :userId ORDER BY rowid DESC LIMIT 1")
    suspend fun latest(userId: String): ConsentEventEntity?

    /** The whole trail, oldest first. This is the audit evidence. */
    @Query("SELECT * FROM consent_events WHERE userId = :userId ORDER BY recordedAt ASC, id ASC")
    fun observeTrail(userId: String): Flow<List<ConsentEventEntity>>
}
