package com.kasiguru.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.ProfileEntity
import com.kasiguru.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileSelectionViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val profiles: StateFlow<List<ProfileEntity>> = profileRepository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectProfile(id: Int, onSelected: () -> Unit) {
        viewModelScope.launch {
            profileRepository.setActiveProfile(id)
            onSelected()
        }
    }

    fun addProfile(name: String, residentName: String) {
        viewModelScope.launch {
            profileRepository.createProfile(name, residentName)
        }
    }

    fun deleteProfile(id: Int) {
        viewModelScope.launch {
            profileRepository.deleteProfile(id)
        }
    }
}
