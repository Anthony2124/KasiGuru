package com.kasiguru.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.KasiGuruDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RewardCelebrationViewModel @Inject constructor(private val db: KasiGuruDatabase) : ViewModel() {
    val pending = db.rewardDao().pendingCelebration().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),null)
    fun dismiss() {
        val id = pending.value?.id ?: return
        viewModelScope.launch { db.rewardDao().acknowledgeCelebration(id) }
    }
}
