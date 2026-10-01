package com.kasiguru.ui.screens.profile

import androidx.lifecycle.*
import com.kasiguru.data.remote.model.PublicProfileDto
import com.kasiguru.data.repository.PublicProfileRepository
import com.kasiguru.domain.gamification.BadgeShowcase
import com.kasiguru.data.local.entity.AchievementEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PublicProfileUiState(val profile: PublicProfileDto? = null, val loading: Boolean = true,
    val error: String? = null, val weeklyRank: Int? = null, val myWeeklyXp: Int = 0,
    val showcase: List<AchievementEntity> = emptyList(), val refreshing: Boolean = false)

@HiltViewModel
class PublicProfileViewModel @Inject constructor(private val repository: PublicProfileRepository, savedStateHandle: SavedStateHandle) : ViewModel() {
    private val uid = savedStateHandle.get<String>("uid").orEmpty()
    private val _state = MutableStateFlow(PublicProfileUiState())
    val state = _state.asStateFlow()
    private var job: Job? = null
    init { refresh() }
    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            val cached = repository.cached(uid)
            _state.update { it.copy(profile = cached, loading = cached == null, refreshing = true, error = null,
                showcase = BadgeShowcase.choose(cached?.badgeIds.orEmpty(), emptyMap()), myWeeklyXp = repository.localWeeklyXp()) }
            try {
                val profile = repository.refresh(uid)
                _state.update { it.copy(profile = profile, loading = false, refreshing = false,
                    showcase = BadgeShowcase.choose(profile?.badgeIds.orEmpty(), emptyMap())) }
                if (profile != null) {
                    val rarity = repository.badgeRarity(BadgeShowcase.candidates(profile.badgeIds).map { it.id })
                    val rank = try { repository.weeklyRank(uid, profile) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { null }
                    _state.update { it.copy(weeklyRank = rank, showcase = BadgeShowcase.choose(profile.badgeIds, rarity)) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(loading = false, refreshing = false, error = "Couldn't refresh this profile. You can try again when you're online.") } }
        }
    }
    fun weeklyXp(profile: PublicProfileDto): Int = if (profile.weekId == repository.currentWeekId()) profile.weeklyXp else 0
}
