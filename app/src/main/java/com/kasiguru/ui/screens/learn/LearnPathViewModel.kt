package com.kasiguru.ui.screens.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.repository.AuthRepository
import com.kasiguru.data.repository.LessonRepository
import com.kasiguru.domain.lesson.TreeSection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LearnPathUiState(
    val isLoading: Boolean = true,
    /** The learning tree with this learner's state applied. */
    val tree: List<TreeSection> = emptyList()
) {
    /** The first section holding a node to tap next, for the tab's subtitle. */
    val currentSection: TreeSection?
        get() = tree.firstOrNull { section -> section.isUnlocked && section.nodes.any { it.isCurrent } }
}

/**
 * Backs the Learn tab, which is the path and nothing else.
 *
 * Deliberately not [LearnViewModel]. That one belongs to Home and, on every construction, checks for
 * an app update, reads announcements and asks the server about approved submissions - work that must
 * happen once per visit to Home, not a second time because the learner also opened the path.
 */
@HiltViewModel
class LearnPathViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearnPathUiState())
    val uiState: StateFlow<LearnPathUiState> = _uiState.asStateFlow()

    init {
        refresh()
        observeAuthForRefresh()
    }

    /**
     * Re-derives the tree. Called on entry and whenever the tab resumes, because the work that
     * changes it happens in the lesson player: a stale path would show a lesson the learner has just
     * finished as still waiting for them.
     */
    fun refresh() {
        viewModelScope.launch {
            val tree = lessonRepository.treeSections()
            _uiState.update { it.copy(tree = tree, isLoading = false) }
        }
    }

    /** A sign-out swaps whose progress is on the device; the old account's path must not linger. */
    private fun observeAuthForRefresh() {
        viewModelScope.launch {
            authRepository.accountState
                .map { it.uid }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    _uiState.update { it.copy(tree = emptyList(), isLoading = true) }
                    refresh()
                }
        }
    }
}
