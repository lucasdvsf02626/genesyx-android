package com.genesyx.app.ui.learn

import androidx.lifecycle.ViewModel
import com.genesyx.app.data.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Learn landing state. Articles are a compile-time constant; what persists is the drip bookkeeping —
 * the first-open anchor every surface gates through
 * ([com.genesyx.app.domain.content.LearnDrip]) and the read slugs. Category filtering is transient
 * UI state and stays in the composable.
 *
 * The `learn_intro_seen` flag is intentionally left in [PreferencesRepository] and untouched: the
 * dismissible first-visit hint it gated is gone, replaced by two permanent entry cards, but the key
 * is already written on real devices and dropping it would be a DataStore migration for no gain.
 */
@HiltViewModel
class LearnViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
) : ViewModel() {
    fun markRead(slug: String) = preferences.markArticleRead(slug)
}
