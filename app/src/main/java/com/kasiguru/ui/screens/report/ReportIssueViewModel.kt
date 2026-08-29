package com.kasiguru.ui.screens.report

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.BuildConfig
import com.kasiguru.data.remote.model.IssueReportDto
import com.kasiguru.data.repository.AuthRepository
import com.kasiguru.data.repository.ReportRepository
import com.kasiguru.util.ImageCompressor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportUiState(
    val category: String = "Bug / System Issue",
    val title: String = "",
    val description: String = "",
    val targetWord: String = "",
    val targetScreen: String = "",
    val photoUri: Uri? = null,
    val photoBase64: String = "",
    val isCompressingPhoto: Boolean = false,
    val reporterName: String = "",
    val reporterEmail: String = "",
    val appVersion: String = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
    val deviceInfo: String = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})",
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val submittedReportId: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ReportIssueViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reportRepository: ReportRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val categories = listOf(
        "Bug / System Issue",
        "Wrong Word / Translation",
        "Grammar / Literature",
        "Audio Issue",
        "Other"
    )

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        val current = authRepository.currentAccount()
        val defaultEmail = current.email ?: ""
        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val defaultName = firebaseUser?.displayName?.takeIf { it.isNotBlank() }
            ?: (if (defaultEmail.isNotEmpty()) defaultEmail.substringBefore('@') else "")
        _uiState.update { it.copy(reporterName = defaultName, reporterEmail = defaultEmail) }
    }

    fun initPrefilled(category: String?, word: String?, screenContext: String?) {
        _uiState.update { state ->
            state.copy(
                category = if (!category.isNullOrBlank() && category in categories) category else state.category,
                targetWord = word?.takeIf { it.isNotBlank() } ?: state.targetWord,
                targetScreen = screenContext?.takeIf { it.isNotBlank() } ?: state.targetScreen,
                title = if (!word.isNullOrBlank() && state.title.isBlank()) {
                    "Issue with word: $word"
                } else state.title
            )
        }
    }

    fun onCategorySelected(category: String) {
        _uiState.update { it.copy(category = category, errorMessage = null) }
    }

    fun onTitleChanged(value: String) {
        _uiState.update { it.copy(title = value, errorMessage = null) }
    }

    fun onDescriptionChanged(value: String) {
        _uiState.update { it.copy(description = value, errorMessage = null) }
    }

    fun onTargetWordChanged(value: String) {
        _uiState.update { it.copy(targetWord = value) }
    }

    fun onTargetScreenChanged(value: String) {
        _uiState.update { it.copy(targetScreen = value) }
    }

    fun onReporterNameChanged(value: String) {
        _uiState.update { it.copy(reporterName = value) }
    }

    fun onPhotoSelected(uri: Uri?) {
        if (uri == null) return
        _uiState.update { it.copy(photoUri = uri, isCompressingPhoto = true, errorMessage = null) }
        viewModelScope.launch {
            val result = ImageCompressor.compressToBase64(context, uri)
            result.onSuccess { base64 ->
                _uiState.update { it.copy(photoBase64 = base64, isCompressingPhoto = false) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        photoUri = null,
                        photoBase64 = "",
                        isCompressingPhoto = false,
                        errorMessage = "Could not attach image: ${error.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun onRemovePhoto() {
        _uiState.update { it.copy(photoUri = null, photoBase64 = "", isCompressingPhoto = false) }
    }

    fun submitReport() {
        val state = _uiState.value

        if (state.title.trim().length < 3) {
            _uiState.update { it.copy(errorMessage = "Please enter a short summary or title (at least 3 characters).") }
            return
        }

        if (state.description.trim().length < 5) {
            _uiState.update { it.copy(errorMessage = "Please provide details about the bug or incorrect word.") }
            return
        }

        if (state.category == "Wrong Word / Translation" && state.targetWord.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please specify the Kasiguranin word that is incorrect.") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch {
            val reportDto = IssueReportDto(
                category = state.category,
                title = state.title.trim(),
                description = state.description.trim(),
                targetWord = state.targetWord.trim(),
                targetScreen = state.targetScreen.trim(),
                photoBase64 = state.photoBase64,
                reporterName = state.reporterName.trim().ifBlank { "Anonymous" },
                reporterEmail = state.reporterEmail.trim(),
                appVersion = state.appVersion,
                deviceInfo = state.deviceInfo
            )

            val result = reportRepository.submitReport(reportDto)
            result.onSuccess { id ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        isSuccess = true,
                        submittedReportId = id,
                        errorMessage = null
                    )
                }
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Failed to submit report: ${ex.localizedMessage ?: "Please check connection and try again."}"
                    )
                }
            }
        }
    }

    fun resetSuccess() {
        _uiState.update {
            ReportUiState(
                reporterName = it.reporterName,
                reporterEmail = it.reporterEmail
            )
        }
    }
}
