package com.kasiguru.ui.navigation

import androidx.lifecycle.ViewModel
import com.kasiguru.domain.audio.MusicMood
import com.kasiguru.util.audio.MusicPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Hands the current screen's music mood to the app-wide [MusicPlayer]. */
@HiltViewModel
class MusicViewModel @Inject constructor(private val musicPlayer: MusicPlayer) : ViewModel() {
    fun setMood(mood: MusicMood) = musicPlayer.setMood(mood)
}
