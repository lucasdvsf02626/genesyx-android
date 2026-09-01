package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.dao.ConsentEventDao
import com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore
import com.genesyx.app.data.local.entity.ConsentEventEntity
import com.genesyx.app.data.local.entity.toDomain
import com.genesyx.app.data.remote.ConsentRemoteDataSource
import com.genesyx.app.data.remote.dto.toDto
import com.genesyx.app.data.remote.dto.toEntity
import com.genesyx.app.domain.consent.ConsentAction
import com.genesyx.app.domain.consent.HealthDataCollectionGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Consent to collect Article 9 health data, held as an append-only trail rather than a flag.
 *
 * [grant] and [withdraw] only ever insert. Nothing here updates or deletes an event, because the
 * trail is the evidence that a grant happened — a withdrawal that overwrote it would destroy the
 * one record worth keeping. Current state is always derived by reading the newest row.
 *
 * **Withdrawal stops collection; it is not an erasure request.** What the device already holds is
 * left exactly where it is. Deleting her data is a separate, explicit action (`deleteAccount`), and
 * conflating the two would mean a woman who wants to pause tracking loses her history to do it.
 *
 * An empty trail reads as permitted. That is deliberate and matches iOS: every install that
 * upgrades into this version has never been offered a consent screen, and defaulting them to
 * refused would silently stop tracking for everyone at once. The one exception: when a sign-in's
 * server pull confirms the account has no answer ANYWHERE, [needsDecision] turns on and the UI
 * asks instead of assuming — an answered prompt is better evidence than a default.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ConsentRepository @Inject constructor(
    private val dao: ConsentEventDao,
    private val remote: ConsentRemoteDataSource,
    private val session: SessionRepository,
    private val store: GenesyxPreferencesDataStore,
    private val logger: Logger,
    @ApplicationScope scope: CoroutineScope,
) : HealthDataCollectionGate {

    /**
     * True when a completed server pull found no consent answer anywhere — locally or on the
     * account. The UI must ask rather than assume permitted (the empty-trail default exists for
     * installs that were never offered the screen, not for accounts the server has confirmed never
     * answered). Cleared the moment any event is recorded.
     *
     * PERSISTED (DataStore), keyed by the uid it applies to: an in-memory flag forgot "undecided"
     * across process death — which read as permitted — and a bare boolean could be inherited by
     * the next account on the device. Sign-out/deletion wipe it with the rest of the prefs file.
     */
    val needsDecision: StateFlow<Boolean> =
        combine(session.userId, store.consentDecisionNeededUid) { uid, neededUid ->
            uid != null && uid == neededUid
        }.stateIn(scope, SharingStarted.Eagerly, false)

    /** Drives the UI — the Profile row's state and the withdrawn banner. */
    val isActive: StateFlow<Boolean> =
        combine(session.userId, store.consentDecisionNeededUid) { uid, neededUid ->
            (uid ?: SessionRepository.LOCAL_USER_ID) to (uid != null && uid == neededUid)
        }
            .flatMapLatest { (uid, pending) ->
                dao.observeTrailByInsertion(uid).map { it.newest().permits(pending) }
            }
            .stateIn(scope, SharingStarted.Eagerly, true)

    /**
     * The authoritative check. Reads the trail rather than [isActive] so it cannot be answered by a
     * seed value during the window between process start and Room's first emission.
     */
    override suspend fun isCollectionPermitted(): Boolean {
        val uid = session.awaitUserId()
        val pending = uid != SessionRepository.LOCAL_USER_ID &&
            store.consentDecisionNeededUid.first() == uid
        return dao.trailByInsertion(uid).newest().permits(pending)
    }

    /**
     * Sign-in: merge the account's server trail with the local one BEFORE any health-data pull or
     * push runs under a consent she may have revoked on another device (or before a reinstall —
     * the defect this fixes: a wiped device had an empty trail, which read as permitted, and the
     * withdrawal she'd recorded was silently reversed).
     *
     * Merge rules: adopt server events missing locally (oldest first, so insertion order keeps
     * tracking chronology); push local events the server lacks (plain INSERT — the table is
     * append-only with no UPDATE policy, and a replayed id collides harmlessly); the newest event
     * by timestamp wins either way. A failed pull changes nothing — the local answer stands until
     * the server can be asked.
     */
    suspend fun refresh(userId: String): Boolean {
        if (userId == SessionRepository.LOCAL_USER_ID) return true
        val remoteTrail = when (val r = remote.list(userId)) {
            is DataResult.Success -> r.data
            else -> {
                logger.w("Consent", "trail pull failed — keeping the local answer")
                return false
            }
        }
        val local = dao.trailByInsertion(userId)
        val localIds = local.map { it.id }.toSet()
        val toAdopt = remoteTrail.filter { it.id !in localIds }
            .map { it.toEntity() }
            .sortedBy { it.recordedAt }
        if (toAdopt.isNotEmpty()) {
            dao.insertAll(toAdopt)
            logger.i("Consent", "adopted ${toAdopt.size} server consent event(s)")
        }
        val remoteIds = remoteTrail.map { it.id }.toSet()
        for (event in local.filter { it.id !in remoteIds }) {
            if (remote.insert(event.toDto()) !is DataResult.Success) {
                logger.w("Consent", "trail push deferred — will retry next sign-in")
            }
        }
        store.setConsentDecisionNeededUid(
            userId.takeIf { remoteTrail.isEmpty() && local.isEmpty() },
        )
        return true
    }

    suspend fun grant() = append(ConsentAction.GRANTED)

    suspend fun withdraw() = append(ConsentAction.WITHDRAWN)

    /**
     * Carries a guest's answer onto the account she just signed into, when that account has never
     * answered for itself.
     *
     * The trail is keyed by user, and a guest's is keyed to `local-user`. Without this, withdrawing
     * as a guest and then signing in left the new id with an empty trail — which reads as permitted
     * (see the class doc) — so collection silently resumed under a consent she had revoked minutes
     * earlier, and the Profile row said "On" without ever asking her again.
     *
     * Appends rather than re-keys: her guest trail is evidence and stays where it is.
     */
    suspend fun adoptGuestDecision(userId: String) {
        if (userId == SessionRepository.LOCAL_USER_ID) return
        // An answer she gave while signed into this account outranks anything from the guest bucket.
        if (dao.latest(userId) != null) return
        val guest = dao.latest(SessionRepository.LOCAL_USER_ID)?.toDomain() ?: return
        append(guest.action, userId)
        logger.i("Consent", "carried the guest's ${guest.action.wire} decision onto the signed-in account")
    }

    private suspend fun append(action: ConsentAction, forUserId: String? = null) {
        val userId = forUserId ?: session.awaitUserId()
        val event = ConsentEventEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            action = action.wire,
            recordedAt = LocalDateTime.now(),
        )
        dao.insert(event)
        // Any answer ends the ask — she has decided. Cleared immediately, before the push, so a
        // crash mid-push cannot leave her being re-asked a question she answered.
        store.setConsentDecisionNeededUid(null)
        logger.i("Consent", "recorded ${action.wire}")
        // Best-effort push; a miss is re-sent by the next sign-in's refresh (append-only, by id).
        if (userId != SessionRepository.LOCAL_USER_ID &&
            remote.insert(event.toDto()) !is DataResult.Success
        ) {
            logger.w("Consent", "event push deferred — will retry next sign-in")
        }
    }

    /**
     * The event that decides the current state: newest by real timestamp (compared as
     * [LocalDateTime], not ISO text — `toString()` drops zero seconds and mis-sorts), insertion
     * order breaking ties. Post-sync the trail is a merge of two devices, so rowid alone is no
     * longer chronology.
     */
    private fun List<ConsentEventEntity>.newest(): ConsentEventEntity? =
        withIndex().maxWithOrNull(compareBy({ it.value.recordedAt }, { it.index }))?.value

    /** No event yet means no answer, which is permitted — unless the server confirmed there is no
     *  answer anywhere, in which case we ask instead of assuming (see [needsDecision]). */
    private fun ConsentEventEntity?.permits(decisionPending: Boolean): Boolean =
        if (this == null) !decisionPending else toDomain().action == ConsentAction.GRANTED
}
