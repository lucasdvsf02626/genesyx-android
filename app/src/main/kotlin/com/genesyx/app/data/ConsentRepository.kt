package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.core.log.Logger
import com.genesyx.app.data.local.dao.ConsentEventDao
import com.genesyx.app.data.local.entity.ConsentEventEntity
import com.genesyx.app.data.local.entity.toDomain
import com.genesyx.app.domain.consent.ConsentAction
import com.genesyx.app.domain.consent.HealthDataCollectionGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
 * refused would silently stop tracking for everyone at once.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ConsentRepository @Inject constructor(
    private val dao: ConsentEventDao,
    private val session: SessionRepository,
    private val logger: Logger,
    @ApplicationScope scope: CoroutineScope,
) : HealthDataCollectionGate {

    /** Drives the UI — the Profile row's state and the withdrawn banner. */
    val isActive: StateFlow<Boolean> =
        session.userId
            .flatMapLatest { uid -> dao.observeLatest(uid ?: SessionRepository.LOCAL_USER_ID) }
            .map { it.permits() }
            .stateIn(scope, SharingStarted.Eagerly, true)

    /**
     * The authoritative check. Reads the trail rather than [isActive] so it cannot be answered by a
     * seed value during the window between process start and Room's first emission.
     */
    override suspend fun isCollectionPermitted(): Boolean =
        dao.latest(session.awaitUserId()).permits()

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
        logger.i("Consent", "carried the guest's ${guest.action.wire} decision onto $userId")
    }

    private suspend fun append(action: ConsentAction, forUserId: String? = null) {
        val userId = forUserId ?: session.awaitUserId()
        dao.insert(
            ConsentEventEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                action = action.wire,
                recordedAt = LocalDateTime.now(),
            ),
        )
        logger.i("Consent", "recorded ${action.wire} for $userId")
    }

    /** No event yet means no answer, which is permitted — see the class doc. */
    private fun ConsentEventEntity?.permits(): Boolean =
        this == null || toDomain().action == ConsentAction.GRANTED
}
