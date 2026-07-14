package com.genesyx.app.ui.learn

import androidx.lifecycle.ViewModel
import com.genesyx.app.data.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Learn landing state. Articles are a compile-time constant, so the only thing worth persisting is
 * whether the first-time hint card has been dismissed. Category filtering is transient UI state and
 * stays in the composable.
 */
@HiltViewModel
class LearnViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
) : ViewModel() {
    val introSeen: StateFlow<Boolean> = preferences.learnIntroSeen

    fun dismissIntro() = preferences.setLearnIntroSeen(true)
}
