package com.genesyx.app.domain.model

/** Lifecycle state of a managed client record. */
enum class ClientStatus { ACTIVE, ARCHIVED }

/**
 * A client record owned by an authenticated account. Domain model for the multi-client scaling
 * path: one owner → many clients, isolated by [ownerUserId] (the Supabase RLS scope key).
 */
data class Client(
    val id: String,
    val ownerUserId: String,
    val displayName: String,
    val email: String? = null,
    val notes: String? = null,
    val status: ClientStatus = ClientStatus.ACTIVE,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
