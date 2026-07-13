package com.genesyx.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.SessionRepository
import com.genesyx.app.domain.content.phaseHeroCopy
import com.genesyx.app.domain.content.phaseHeroSubtext
import com.genesyx.app.domain.content.phaseHeroText
import com.genesyx.app.domain.content.phaseSubLabel
import com.genesyx.app.domain.content.phaseTags
import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.model.CycleSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** Home screen UI state. Mirrors the web Home (greeting, cycle hero, hydration, streak). */
data class HomeUiState(
    val userName: String = "Guest",
    val signedIn: Boolean = false,
    val greeting: String = "Good morning",
    val settings: CycleSettings? = null,
    val cycleSetUp: Boolean = false,
    val cycleEyebrow: String = "TODAY",
    val cycleHeadline: String = "Set up your cycle",
    val cycleSub: String = "Add your last period date to get personalised insights.",
    val cycleTags: List<String> = emptyList(),
    val todayFocusTitle: String? = null,
    val todayFocusBody: String? = null,
    val hydrationLitres: Float? = null,
    /** A general daily recommendation, not a goal the user set. */
    val hydrationGoalLitres: Float = 2.4f,
    val streakDays: Int? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(
            cycleRepository.settings,
            dailyLogRepository.logByDate,
            sessionRepository.displayName,
            sessionRepository.isSignedIn,
        ) { settings, _, displayName, signedIn ->
            buildState(settings, displayName, signedIn)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = buildState(cycleRepository.settings.value, sessionRepository.displayName.value, sessionRepository.isSignedIn.value),
        )

    fun saveCycleSettings(settings: CycleSettings) = cycleRepository.upsert(settings)

    private fun buildState(settings: CycleSettings?, displayName: String?, signedIn: Boolean): HomeUiState {
        val today = LocalDate.now()
        val waterMl = dailyLogRepository.waterMlOn(today)
        val base = HomeUiState(
            userName = displayName ?: "Guest",
            signedIn = signedIn,
            greeting = greetingFor(LocalTime.now()),
            settings = settings,
            hydrationLitres = if (waterMl > 0) waterMl / 1000f else null,
            streakDays = dailyLogRepository.streak(today),
        )
        if (settings == null) return base

        val info = CycleEngine.getCyclePhase(settings, today)
        val inFertile = info.dayOfCycle in info.fertileWindow
        val focus = phaseHeroCopy.getValue(info.phase).focus
        return base.copy(
            cycleSetUp = true,
            cycleEyebrow = "DAY ${info.dayOfCycle} · ${phaseSubLabel(info.phase, inFertile).uppercase()}",
            cycleHeadline = phaseHeroText(info.phase, inFertile),
            cycleSub = phaseHeroSubtext(info.phase, inFertile),
            cycleTags = phaseTags(info.phase, inFertile),
            todayFocusTitle = focus.title,
            todayFocusBody = focus.body,
        )
    }

    private fun greetingFor(time: LocalTime): String = when (time.hour) {
        in 0..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}
