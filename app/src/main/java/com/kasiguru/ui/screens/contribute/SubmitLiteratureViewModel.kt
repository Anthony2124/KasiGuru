package com.kasiguru.ui.screens.contribute

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.remote.model.LiteratureSubmissionDto
import com.kasiguru.data.repository.SubmissionRepository
import com.kasiguru.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
    val pdfUri: Uri? = null,
    val pdfFileName: String = "",
    val pdfFileSize: Long = 0L,
    val pdfBase64: String = "",
    val isReadingPdf: Boolean = false,
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

    fun onPdfSelected(context: Context, uri: Uri) {
        _uiState.value = _uiState.value.copy(isReadingPdf = true, errorMessage = null)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver

                // 1. Determine file name and size
                var fileName = "story.pdf"
                var fileSize = 0L

                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            val name = it.getString(nameIndex)
                            if (!name.isNullOrBlank()) fileName = name
                        }
                        val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex >= 0) {
                            fileSize = it.getLong(sizeIndex)
                        }
                    }
                }

                // 2. Read bytes
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Unable to open file.")
                val bytes = inputStream.use { it.readBytes() }

                if (fileSize <= 0L) {
                    fileSize = bytes.size.toLong()
                }

                // 3. Size check (<= 500 KB to comfortably fit Firestore document limits)
                if (bytes.size > MAX_PDF_SIZE_BYTES) {
                    val sizeKb = bytes.size / 1024
                    _uiState.value = _uiState.value.copy(
                        isReadingPdf = false,
                        errorMessage = "PDF is too large ($sizeKb KB). Please upload a file under 500 KB."
                    )
                    return@launch
                }

                // 4. Validate PDF header magic bytes "%PDF"
                val isPdfHeader = bytes.size >= 4 &&
                    bytes[0] == 0x25.toByte() && // '%'
                    bytes[1] == 0x50.toByte() && // 'P'
                    bytes[2] == 0x44.toByte() && // 'D'
                    bytes[3] == 0x46.toByte()    // 'F'

                if (!isPdfHeader && !fileName.endsWith(".pdf", ignoreCase = true)) {
                    _uiState.value = _uiState.value.copy(
                        isReadingPdf = false,
                        errorMessage = "The selected file is not a valid PDF document."
                    )
                    return@launch
                }

                // 5. Encode to Base64 data URI
                val base64 = "data:application/pdf;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)

                _uiState.value = _uiState.value.copy(
                    pdfUri = uri,
                    pdfFileName = fileName,
                    pdfFileSize = fileSize,
                    pdfBase64 = base64,
                    isReadingPdf = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isReadingPdf = false,
                    errorMessage = "Failed to load PDF: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun onRemovePdf() {
        _uiState.value = _uiState.value.copy(
            pdfUri = null,
            pdfFileName = "",
            pdfFileSize = 0L,
            pdfBase64 = "",
            isReadingPdf = false
        )
    }

    fun onPageChanged(index: Int, page: LiteraturePageDraft) {
        val pages = _uiState.value.pages.toMutableList()
        if (index !in pages.indices) return
        pages[index] = page
        _uiState.value = _uiState.value.copy(pages = pages, errorMessage = null)
    }

    fun addPage() {
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
        if (state.isLoading || state.isReadingPdf) return
        if (now - lastSubmittedAt < SUBMISSION_COOLDOWN_MS) {
            _uiState.value = state.copy(
                errorMessage = "Please wait a moment before submitting another piece."
            )
            return
        }
        if (state.pdfBase64.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please attach a PDF file of your story or poem.")
            return
        }
        if (state.title.isBlank() && state.titleKasiguranin.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please give the piece a title.")
            return
        }

        val nonEmptyPages = state.pages.filter {
            it.kasiguranin.isNotBlank() || it.tagalog.isNotBlank() || it.english.isNotBlank()
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val pagesJson = if (nonEmptyPages.isEmpty()) {
                "[]"
            } else {
                JSONArray().apply {
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
            }

            val submission = LiteratureSubmissionDto(
                title = state.title.trim(),
                titleKasiguranin = state.titleKasiguranin.trim(),
                pagesJson = pagesJson,
                contributorName = if (state.contributorName.isBlank()) "Anonymous Contributor" else state.contributorName.trim(),
                pdfBase64 = state.pdfBase64,
                pdfFileName = state.pdfFileName.trim(),
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
        const val MAX_PDF_SIZE_BYTES = 500 * 1024 // 500 KB limit for Firestore base64 storage
    }

    private var lastSubmittedAt: Long = 0L
}

