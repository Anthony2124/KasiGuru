package com.kasiguru.ui.screens.contribute

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.remote.model.LiteratureSubmissionDto
import com.kasiguru.data.repository.SubmissionRepository
import com.kasiguru.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/** One page of a submitted story or poem, mirroring [com.kasiguru.data.local.entity.StoryPage]. */
data class LiteraturePageDraft(
    val kasiguranin: String = "",
    val tagalog: String = "",
    val english: String = ""
)

data class SubmitLiteratureUiState(
    val title: String = "",
    val titleKasiguranin: String = "",
    val pages: List<LiteraturePageDraft> = listOf(LiteraturePageDraft()),
    val contributorName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class SubmitLiteratureViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubmitLiteratureUiState())
    val uiState: StateFlow<SubmitLiteratureUiState> = _uiState.asStateFlow()

    fun onTitleChanged(value: String) {
        _uiState.value = _uiState.value.copy(title = value, errorMessage = null)
    }

    fun onTitleKasiguraninChanged(value: String) {
        _uiState.value = _uiState.value.copy(titleKasiguranin = value, errorMessage = null)
    }

    fun onContributorNameChanged(value: String) {
        _uiState.value = _uiState.value.copy(contributorName = value)
    }

    fun onPageChanged(index: Int, page: LiteraturePageDraft) {
        val pages = _uiState.value.pages.toMutableList()
        if (index !in pages.indices) return
        pages[index] = page
        _uiState.value = _uiState.value.copy(pages = pages, errorMessage = null)
    }

    fun addPage() {
        // Firestore's 1 MiB document ceiling is nowhere close at plain text volumes, but a
        // runaway page count from a mis-tap is still worth a floor - 40 pages is already a long
        // story for this corpus.
        if (_uiState.value.pages.size >= 40) return
        _uiState.value = _uiState.value.copy(pages = _uiState.value.pages + LiteraturePageDraft())
    }

    fun removePage(index: Int) {
        val pages = _uiState.value.pages.toMutableList()
        if (pages.size <= 1 || index !in pages.indices) return
        pages.removeAt(index)
        _uiState.value = _uiState.value.copy(pages = pages)
    }

    fun submitLiterature() {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        if (state.isLoading) return
        if (now - lastSubmittedAt < SUBMISSION_COOLDOWN_MS) {
            _uiState.value = state.copy(
                errorMessage = "Please wait a moment before submitting another piece."
            )
            return
        }
        if (state.title.isBlank() && state.titleKasiguranin.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please give the piece a title.")
            return
        }
        val nonEmptyPages = state.pages.filter {
            it.kasiguranin.isNotBlank() || it.tagalog.isNotBlank() || it.english.isNotBlank()
        }
        if (nonEmptyPages.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Please write at least one page.")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val pagesJson = JSONArray().apply {
                nonEmptyPages.forEachIndexed { index, page ->
                    put(
                        JSONObject().apply {
                            put("pageNumber", index + 1)
                            put("kasiguranin", page.kasiguranin.trim())
                            put("tagalog", page.tagalog.trim())
                            put("english", page.english.trim())
                        }
                    )
                }
            }.toString()

            val submission = LiteratureSubmissionDto(
                title = state.title.trim(),
                titleKasiguranin = state.titleKasiguranin.trim(),
                pagesJson = pagesJson,
                contributorName = if (state.contributorName.isBlank()) "Anonymous Contributor" else state.contributorName.trim(),
                status = "pending",
                submittedAt = System.currentTimeMillis()
            )

            val result = submissionRepository.submitLiterature(submission)
            result.fold(
                onSuccess = {
                    lastSubmittedAt = System.currentTimeMillis()
                    userProgressRepository.incrementSubmissionsMade()
                    _uiState.value = SubmitLiteratureUiState(isSuccess = true)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to submit. Please try again."
                    )
                }
            )
        }
    }

    fun resetSuccess() {
        _uiState.value = SubmitLiteratureUiState()
    }

    companion object {
        private const val SUBMISSION_COOLDOWN_MS = 30_000L
    }

    private var lastSubmittedAt: Long = 0L
}
