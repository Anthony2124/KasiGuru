package com.kasiguru.ui.screens.xp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.KasiGuruDatabase
import com.kasiguru.data.local.dao.GameLevelDao
import com.kasiguru.data.local.entity.UserProgressEntity
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.domain.gamification.XpSource
import com.kasiguru.domain.gamification.XpSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class XpUiState(
    val isLoading: Boolean = true,
    val progress: UserProgressEntity = UserProgressEntity(),
    /** The seven days ending today, oldest first. */
    val week: List<Pair<LocalDate, Int>> = emptyList(),
    val bySource: Map<XpSource, Int> = emptyMap(),
    val gameStars: Int = 0,
    val gameStarsPossible: Int = 0
) {
    /** Today's ledger only counts if it is today's; yesterday's figure is not today's progress. */
    val todayXp: Int
        get() = if (progress.dailyXpDate == LocalDate.now().toString()) progress.dailyXpEarned else 0
    val weekXp: Int get() = week.sumOf { it.second }
}

@HiltViewModel
class XpViewModel @Inject constructor(
    userProgressRepository: UserProgressRepository,
    database: KasiGuruDatabase,
    gameLevelDao: GameLevelDao
) : ViewModel() {

    private val levelCount = flow { emit(gameLevelDao.getLevelCount()) }

    val uiState: StateFlow<XpUiState> = combine(
        userProgressRepository.getUserProgress().filterNotNull(),
        database.rewardDao().observe(),
        gameLevelDao.getTotalStarsFlow(),
        levelCount
    ) { progress, receipts, stars, levels ->
        val records = receipts.map { it.record() }
        XpUiState(
            isLoading = false,
            progress = progress,
            week = XpSummary.lastSevenDays(records, LocalDate.now()),
            bySource = XpSummary.bySource(records),
            gameStars = stars,
            gameStarsPossible = levels * 3
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), XpUiState())
}
