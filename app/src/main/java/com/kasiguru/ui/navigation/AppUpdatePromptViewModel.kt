package com.kasiguru.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.BuildConfig
import com.kasiguru.data.remote.model.AppReleaseDto
import com.kasiguru.data.repository.AppUpdateRepository
import com.kasiguru.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUpdatePromptState(
    /** The newest release above the installed version, or null when the app is up to date. */
    val release: AppReleaseDto? = null,
    /** The pop-up is open. Always, for a required update. */
    val showDialog: Boolean = false,
    /** Home's slim reminder, after "Later" and until the learner dismisses this version. */
    val showBanner: Boolean = false
)

/**
 * Backs the app-wide update pop-up and Home's slim reminder, from one check per app start.
 *
 * Lives at the navigation shell, beside the celebrations, so a required update covers every screen
 * rather than only Home. An optional update pops up once per version; after "Later" it stays as a
 * banner on Home until installed or put away with its ✕.
 */
@HiltViewModel
class AppUpdatePromptViewModel @Inject constructor(
    private val appUpdateRepository: AppUpdateRepository,
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AppUpdatePromptState())
    val state: StateFlow<AppUpdatePromptState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val latest = appUpdateRepository.getLatestRelease().getOrNull() ?: return@launch
            if (latest.versionCode <= BuildConfig.VERSION_CODE) return@launch
            val prompted = preferences.promptedUpdateVersion.first()
            val dismissed = preferences.dismissedUpdateVersion.first()
            _state.value = AppUpdatePromptState(
                release = latest,
                showDialog = latest.forceUpdate || latest.versionCode > prompted,
                showBanner = !latest.forceUpdate && latest.versionCode > dismissed
            )
        }
    }

    /** "Later": close the pop-up, remember it was shown, and leave the banner on Home. */
    fun later() {
        val release = _state.value.release ?: return
        if (release.forceUpdate) return
        _state.update { it.copy(showDialog = false) }
        viewModelScope.launch { preferences.setPromptedUpdateVersion(release.versionCode) }
    }

    /** The banner's "Update": the pop-up again, with the notes and the download. */
    fun openDialog() {
        if (_state.value.release != null) _state.update { it.copy(showDialog = true) }
    }

    /** The banner's ✕: this version is put away until a newer one is released. */
    fun dismissBanner() {
        val release = _state.value.release ?: return
        _state.update { it.copy(showBanner = false) }
        viewModelScope.launch {
            preferences.setPromptedUpdateVersion(release.versionCode)
            preferences.setDismissedUpdateVersion(release.versionCode)
        }
    }
}
