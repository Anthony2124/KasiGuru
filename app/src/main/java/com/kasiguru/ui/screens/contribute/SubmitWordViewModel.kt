package com.kasiguru.ui.screens.contribute

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.remote.model.WordSubmissionDto
import com.kasiguru.data.repository.SubmissionRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.domain.contribute.DuplicateMatch
import com.kasiguru.domain.contribute.DuplicateWordCheck
import com.kasiguru.domain.contribute.ExistingEntry
import com.kasiguru.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SubmitWordUiState(
    val kasiguranin: String = "",
    val tagalog: String = "",
    val english: String = "",
    val rootForm: String = "",
    val category: String = "Greetings & Essentials",
    val partOfSpeech: String = "",
    val ipaNotation: String = "",
    val exampleSentence: String = "",
    val pastTense: String = "",
    val presentTense: String = "",
    val futureTense: String = "",
    val contributorName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    /**
     * Dictionary entries the word being typed may already be repeating, strongest first.
     * Rendered as a caution beneath the headword field; never blocks typing.
     */
    val duplicateMatches: List<DuplicateMatch> = emptyList(),
    /** True while the confirmation dialog for an already-recorded headword is up. */
    val showDuplicateConfirm: Boolean = false
)

@HiltViewModel
class SubmitWordViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository,
    private val userProgressRepository: UserProgressRepository,
    private val vocabularyRepository: VocabularyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubmitWordUiState())
    val uiState: StateFlow<SubmitWordUiState> = _uiState.asStateFlow()

    val categories = Constants.VocabCategories.ALL
    val partsOfSpeech = listOf("Noun", "Verb", "Adjective", "Adverb", "Pronoun", "Preposition", "Conjunction / Connector", "Interjection", "Marker & Particle")

    fun onKasiguraninChanged(value: String) {
        _uiState.value = _uiState.value.copy(kasiguranin = value, errorMessage = null)
        scheduleDuplicateCheck()
    }

    fun onTagalogChanged(value: String) {
        _uiState.value = _uiState.value.copy(tagalog = value, errorMessage = null)
    }

    fun onEnglishChanged(value: String) {
        _uiState.value = _uiState.value.copy(english = value, errorMessage = null)
        // The gloss is what separates this contributor's word from a homonym of it, so the notice
        // has to re-grade when they type the meaning, not only when they type the headword.
        scheduleDuplicateCheck()
    }

    fun onRootFormChanged(value: String) {
        _uiState.value = _uiState.value.copy(rootForm = value)
    }

    fun onCategoryChanged(value: String) {
        _uiState.value = _uiState.value.copy(category = value)
    }

    fun onPartOfSpeechChanged(value: String) {
        _uiState.value = _uiState.value.copy(partOfSpeech = value)
    }

    fun onIpaNotationChanged(value: String) {
        _uiState.value = _uiState.value.copy(ipaNotation = value)
    }

    fun onExampleSentenceChanged(value: String) {
        _uiState.value = _uiState.value.copy(exampleSentence = value)
    }

    fun onPastTenseChanged(value: String) {
        _uiState.value = _uiState.value.copy(pastTense = value)
    }

    fun onPresentTenseChanged(value: String) {
        _uiState.value = _uiState.value.copy(presentTense = value)
    }

    fun onFutureTenseChanged(value: String) {
        _uiState.value = _uiState.value.copy(futureTense = value)
    }

    fun onContributorNameChanged(value: String) {
        _uiState.value = _uiState.value.copy(contributorName = value)
    }

    /**
     * Sends the submission, pausing once on a headword the dictionary already carries.
     *
     * @param confirmedDuplicate set by the confirmation dialog's "Submit anyway". A contributor who
     *   has read the existing entries and still means to submit is usually recording a homonym --
     *   `baga` is lungs, swollen and ember -- which the dictionary genuinely wants, so the caution
     *   asks once and then gets out of the way rather than refusing.
     */
    fun submitWord(confirmedDuplicate: Boolean = false) {
        val state = _uiState.value
        // Client-side rate limit: one submission per cooldown window.
        // (True server-side rate limiting needs Cloud Functions / the Blaze plan.)
        val now = System.currentTimeMillis()
        if (state.isLoading) return
        if (now - lastSubmittedAt < SUBMISSION_COOLDOWN_MS) {
            _uiState.value = state.copy(
                errorMessage = "Please wait a moment before submitting another word."
            )
            return
        }
        if (state.kasiguranin.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter the Kasiguranin word")
            return
        }
        if (state.tagalog.isBlank() && state.english.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter either Tagalog or English definition")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null, showDuplicateConfirm = false)

        viewModelScope.launch {
            // Re-checked here rather than trusting whatever the debounced pass last published: a
            // pasted word followed by an immediate tap can reach this before the 250ms pass has
            // even started, and that is exactly the submission most likely to be a copied duplicate.
            if (!confirmedDuplicate) {
                duplicateCheckJob?.cancel()
                val corpus = loadCorpus()
                val matches = withContext(Dispatchers.Default) {
                    DuplicateWordCheck.find(state.kasiguranin, state.english, corpus)
                }
                if (DuplicateWordCheck.needsConfirmation(matches)) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        duplicateMatches = matches,
                        showDuplicateConfirm = true
                    )
                    return@launch
                }
                _uiState.value = _uiState.value.copy(duplicateMatches = matches)
            }

            val submission = WordSubmissionDto(
                kasiguranin = state.kasiguranin.trim(),
                tagalog = state.tagalog.trim(),
                english = state.english.trim(),
                rootForm = state.rootForm.trim(),
                category = state.category,
                partOfSpeech = state.partOfSpeech,
                ipaNotation = state.ipaNotation.trim(),
                exampleSentence = state.exampleSentence.trim(),
                pastTense = state.pastTense.trim(),
                presentTense = state.presentTense.trim(),
                futureTense = state.futureTense.trim(),
                contributorName = if (state.contributorName.isBlank()) "Anonymous Contributor" else state.contributorName.trim(),
                status = "pending",
                submittedAt = System.currentTimeMillis()
            )

            val result = submissionRepository.submitWord(submission)
            result.fold(
                onSuccess = {
                    lastSubmittedAt = System.currentTimeMillis()
                    userProgressRepository.incrementSubmissionsMade()
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

    fun dismissDuplicateConfirm() {
        _uiState.value = _uiState.value.copy(showDuplicateConfirm = false)
    }

    fun resetSuccess() {
        _uiState.value = SubmitWordUiState()
    }

    /**
     * Re-runs the duplicate check a beat after the last keystroke.
     *
     * Debounced rather than run per character because the comparison is a sweep of the whole corpus
     * -- it cannot be a SQL lookup, since matching has to fold the schwa's three spellings and the
     * corpus's inconsistent accents first -- and because a notice that appears while someone is
     * halfway through a word is noise. Each keystroke cancels the pending pass, so only a settled
     * spelling is ever checked.
     */
    private fun scheduleDuplicateCheck() {
        duplicateCheckJob?.cancel()
        duplicateCheckJob = viewModelScope.launch {
            delay(DUPLICATE_CHECK_DEBOUNCE_MS)
            val state = _uiState.value
            val corpus = loadCorpus()
            val matches = withContext(Dispatchers.Default) {
                DuplicateWordCheck.find(state.kasiguranin, state.english, corpus)
            }
            // The form may have been reset or moved on while this pass ran; only publish matches
            // that still describe what is actually in the fields.
            val current = _uiState.value
            if (current.kasiguranin == state.kasiguranin && current.english == state.english) {
                _uiState.value = current.copy(duplicateMatches = matches)
            }
        }
    }

    /**
     * The on-device dictionary, read once per form session.
     *
     * Held rather than re-queried because the corpus does not change while one word is being filled
     * in, and re-reading twelve hundred rows behind every keystroke would make the field stutter on
     * the low-end phones this app is built for. A content sync landing mid-form is the acceptable
     * cost: the notice is advisory, and admin review still compares against the live dictionary.
     */
    private suspend fun loadCorpus(): List<ExistingEntry> {
        corpus?.let { return it }
        // A failed read must not break the form. The check is a courtesy on top of submitting;
        // losing it silently is far better than refusing a contribution because a query threw.
        val loaded = runCatching {
            vocabularyRepository.getAllHeadwords().map {
                ExistingEntry(
                    kasiguranin = it.kasiguranin,
                    tagalog = it.tagalog,
                    english = it.english,
                    category = it.category
                )
            }
        }.getOrDefault(emptyList())
        corpus = loaded
        return loaded
    }

    companion object {
        private const val SUBMISSION_COOLDOWN_MS = 30_000L
        private const val DUPLICATE_CHECK_DEBOUNCE_MS = 250L
    }

    private var lastSubmittedAt: Long = 0L
    private var duplicateCheckJob: Job? = null
    private var corpus: List<ExistingEntry>? = null
}
