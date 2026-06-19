package com.genesyx.app.ui

import androidx.lifecycle.ViewModel
import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** App-level state: exposes the theme preference so MainActivity can pick light/dark/system. */
@HiltViewModel
class AppViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeMode
}
