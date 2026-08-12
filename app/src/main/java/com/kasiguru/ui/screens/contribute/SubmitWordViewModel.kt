package com.kasiguru.ui.screens.contribute

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.remote.model.WordSubmissionDto
import com.kasiguru.data.repository.SubmissionRepository
import com.kasiguru.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubmitWordUiState(
    val kasiguranin: String = "",
    val tagalog: String = "",
    val english: String = "",
    val rootForm: String = "",
    val category: String = "Greetings & Essentials",
    val ipaNotation: String = "",
    val exampleSentence: String = "",
    val contributorName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class SubmitWordViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubmitWordUiState())
    val uiState: StateFlow<SubmitWordUiState> = _uiState.asStateFlow()

    val categories = Constants.VocabCategories.ALL

    fun onKasiguraninChanged(value: String) {
        _uiState.value = _uiState.value.copy(kasiguranin = value, errorMessage = null)
    }

    fun onTagalogChanged(value: String) {
        _uiState.value = _uiState.value.copy(tagalog = value, errorMessage = null)
    }

    fun onEnglishChanged(value: String) {
        _uiState.value = _uiState.value.copy(english = value, errorMessage = null)
    }

    fun onRootFormChanged(value: String) {
        _uiState.value = _uiState.value.copy(rootForm = value)
    }

    fun onCategoryChanged(value: String) {
        _uiState.value = _uiState.value.copy(category = value)
    }

    fun onIpaNotationChanged(value: String) {
        _uiState.value = _uiState.value.copy(ipaNotation = value)
    }

    fun onExampleSentenceChanged(value: String) {
        _uiState.value = _uiState.value.copy(exampleSentence = value)
    }

    fun onContributorNameChanged(value: String) {
        _uiState.value = _uiState.value.copy(contributorName = value)
    }

    fun submitWord() {
        val state = _uiState.value
        if (state.kasiguranin.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter the Kasiguranin word")
            return
        }
        if (state.tagalog.isBlank() && state.english.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter either Tagalog or English definition")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val submission = WordSubmissionDto(
                kasiguranin = state.kasiguranin.trim(),
                tagalog = state.tagalog.trim(),
                english = state.english.trim(),
                rootForm = state.rootForm.trim(),
                category = state.category,
                ipaNotation = state.ipaNotation.trim(),
                exampleSentence = state.exampleSentence.trim(),
                contributorName = if (state.contributorName.isBlank()) "Anonymous Contributor" else state.contributorName.trim(),
                status = "pending",
                submittedAt = System.currentTimeMillis()
            )

            val result = submissionRepository.submitWord(submission)
            result.fold(
                onSuccess = {
                    _uiState.value = SubmitWordUiState(isSuccess = true)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to submit word. Please try again."
                    )
                }
            )
        }
    }

    fun resetSuccess() {
        _uiState.value = SubmitWordUiState()
    }
}
