package com.genesyx.app.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.ClientRepository
import com.genesyx.app.domain.model.Client
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Drives the Clients screen off [ClientRepository] (local-first Room, owner-scoped, paginated). This
 * surfaces the multi-client scaling seam: the same list works for 5 or 500 records, and is ready to
 * sync to Supabase once the remote datasource is wired.
 */
@HiltViewModel
class ClientsViewModel @Inject constructor(
    private val repository: ClientRepository,
) : ViewModel() {

    val clients: StateFlow<List<Client>> =
        repository.clients.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addClient(name: String, email: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.upsert(
                Client(
                    id = UUID.randomUUID().toString(),
                    ownerUserId = "", // repository stamps the real owner
                    displayName = trimmed,
                    email = email?.trim()?.ifBlank { null },
                ),
            )
        }
    }

    fun deleteClient(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    /** Demo helper: seed sample clients so the 100-record scale path is easy to see and test. */
    fun seedDemo(count: Int = 100) {
        viewModelScope.launch {
            val existing = repository.count()
            repeat(count) { i ->
                val n = existing + i + 1
                repository.upsert(
                    Client(
                        id = UUID.randomUUID().toString(),
                        ownerUserId = "",
                        displayName = "Demo Client %03d".format(n),
                        email = "client$n@example.com",
                    ),
                )
            }
        }
    }
}
