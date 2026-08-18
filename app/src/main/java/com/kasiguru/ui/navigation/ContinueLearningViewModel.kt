package com.kasiguru.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.repository.LessonRepository
import com.kasiguru.domain.lesson.LessonRef
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the docked "continue" action, which is available from every top-level tab.
 *
 * Kept deliberately tiny and separate from [com.kasiguru.ui.screens.learn.LearnViewModel]: the button
 * lives in the navigation shell rather than on any one screen, so it must not depend on a screen's
 * view model being alive.
 */
@HiltViewModel
class ContinueLearningViewModel @Inject constructor(
    private val lessonRepository: LessonRepository
) : ViewModel() {

    private val _nextLesson = MutableStateFlow<LessonRef?>(null)
    val nextLesson: StateFlow<LessonRef?> = _nextLesson.asStateFlow()

    init {
        refresh()
    }

    /** Re-resolves the next lesson. Called after a lesson finishes so the action keeps moving on. */
    fun refresh() {
        viewModelScope.launch {
            _nextLesson.value = lessonRepository.nextLesson()
        }
    }
}
