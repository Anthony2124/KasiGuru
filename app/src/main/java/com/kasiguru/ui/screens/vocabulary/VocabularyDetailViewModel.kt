package com.kasiguru.ui.screens.vocabulary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.remote.model.SentenceSubmissionDto
import com.kasiguru.data.repository.SubmissionRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.domain.contribute.exampleSentenceProblem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VocabularyDetailUiState(
    val word: VocabularyEntity? = null,
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    /** The "Add an example sentence" form: what is wrong with the draft, if anything. */
    val sentenceProblem: String? = null,
    val sendingSentence: Boolean = false,
    /** Sent for review this visit; the form gives way to a thank-you. */
    val sentenceSent: Boolean = false
)

@HiltViewModel
class VocabularyDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vocabularyRepository: VocabularyRepository,
    private val submissionRepository: SubmissionRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val wordId: Int = checkNotNull(savedStateHandle["wordId"])

    private val _uiState = MutableStateFlow(VocabularyDetailUiState())
    val uiState: StateFlow<VocabularyDetailUiState> = _uiState.asStateFlow()

    val allWords: StateFlow<List<VocabularyEntity>> = vocabularyRepository.getAllVocabulary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadWord()
    }

    private fun loadWord() {
        viewModelScope.launch {
            val word = vocabularyRepository.getVocabularyById(wordId)
            _uiState.value = if (word != null) {
                _uiState.value.copy(word = word, isLoading = false, notFound = false)
            } else {
                _uiState.value.copy(word = null, isLoading = false, notFound = true)
            }
        }
    }

    /**
     * Sends an example sentence for a word that has none to the verifiers' queue. It reaches the
     * dictionary, lessons and sentence games only once a verifier approves it.
     */
    fun submitExampleSentence(sentence: String, translation: String) {
        val state = _uiState.value
        val word = state.word ?: return
        if (state.sendingSentence) return
        exampleSentenceProblem(word, sentence, translation)?.let {
            _uiState.value = state.copy(sentenceProblem = it)
            return
        }
        _uiState.value = state.copy(sentenceProblem = null, sendingSentence = true)
        viewModelScope.launch {
            val progress = userProgressRepository.getUserProgressOnce()
            val name = progress?.fullName?.ifBlank { progress.userName }?.ifBlank { null } ?: "Anonymous"
            val result = submissionRepository.submitSentence(
                SentenceSubmissionDto(
                    kasiguranin = word.kasiguranin,
                    english = word.english,
                    sentence = sentence,
                    translation = translation,
                    contributorName = name
                )
            )
            _uiState.value = if (result.isSuccess) {
                userProgressRepository.incrementSubmissionsMade()
                _uiState.value.copy(sendingSentence = false, sentenceSent = true)
            } else {
                _uiState.value.copy(
                    sendingSentence = false,
                    sentenceProblem = "Couldn't send it. Check your connection and try again."
                )
            }
        }
    }

    fun clearSentenceProblem() {
        _uiState.value = _uiState.value.copy(sentenceProblem = null)
    }

    fun markWordAsLearned() {
        viewModelScope.launch {
            vocabularyRepository.markAsLearned(wordId)
            loadWord()
        }
    }
}
