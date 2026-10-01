package com.kasiguru.ui.screens.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.KasiGuruDatabase
import com.kasiguru.data.local.entity.*
import com.kasiguru.data.repository.GamificationRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.domain.gamification.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyProgress(val family: BadgeFamily, val rows: List<AchievementEntity>) {
    val current get() = rows.lastOrNull { it.isUnlocked }
    val next get() = rows.firstOrNull { !it.isUnlocked }
    val value get() = rows.maxOfOrNull { it.currentValue } ?: 0
    val tier get() = current?.id?.let(BadgeCatalog::tierFor) ?: BadgeTier.BEGINNER
}

data class AchievementsUiState(
    val achievements: List<AchievementEntity> = emptyList(),
    val legacy: List<AchievementEntity> = emptyList(),
    val progress: UserProgressEntity? = null,
    val normalization: ProgressNormalizationEntity? = null,
    val isLoading: Boolean = true
) {
    val unlockedCount get() = achievements.count { it.isUnlocked }
    val families get() = BadgeCatalog.families.map { family ->
        FamilyProgress(family, achievements.filter { BadgeCatalog.familyFor(it.id) == family }.sortedBy { it.requiredValue })
    }
}

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    repository: UserProgressRepository,
    private val gamification: GamificationRepository,
    private val db: KasiGuruDatabase
) : ViewModel() {
    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState = _uiState.asStateFlow()
    init {
        viewModelScope.launch {
            gamification.ensureNormalized()
            combine(repository.getAllAchievements(), repository.getLegacyAchievements(),
                repository.getUserProgress(), db.rewardDao().observeNormalization()) { rows, legacy, progress, report ->
                AchievementsUiState(rows, legacy, progress, report, false)
            }.collect { _uiState.value = it }
        }
    }
    fun pin(id: String) { viewModelScope.launch { gamification.pin(id) } }
    fun acknowledgeNormalization() { viewModelScope.launch { db.rewardDao().acknowledge() } }
}
