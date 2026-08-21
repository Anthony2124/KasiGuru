package com.kasiguru.ui.screens.stories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.StoryEntity
import com.kasiguru.data.local.entity.StoryPage
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.StoryRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.remote.StoryImageRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import javax.inject.Inject

@HiltViewModel
class StoryReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val storyRepository: StoryRepository,
    private val userProgressRepository: UserProgressRepository,
    private val vocabularyRepository: VocabularyRepository,
    private val storyImageRepository: StoryImageRepository
) : ViewModel() {

    private val storyId: Int = checkNotNull(savedStateHandle["storyId"])

    private val _uiState = MutableStateFlow(StoryReaderUiState())
    val uiState: StateFlow<StoryReaderUiState> = _uiState.asStateFlow()

    init {
        loadStory()
        viewModelScope.launch {
            val words = vocabularyRepository.getAllVocabularyOnce()
            _uiState.value = _uiState.value.copy(vocabulary = words)
        }
    }

    /**
     * A real dictionary lookup for a word tapped in a story page, matched against the actual corpus
     * instead of the "Tap for meaning" placeholder previously fabricated for every tap.
     */
    fun findWord(rawWord: String): VocabularyEntity? {
        val normalized = rawWord.replace(Regex("[^a-zA-ZáéíóúÁÉÍÓÚñÑ]"), "")
        return _uiState.value.vocabulary.firstOrNull { it.kasiguranin.equals(normalized, ignoreCase = true) }
    }

    /**
     * Loads the picture for a page and, so a page turn does not stall, the one after it. Deliberately
     * not the whole story: at roughly 150 KB a picture, prefetching twelve pages for a learner who
     * reads two would spend their data on nothing.
     */
    private fun ensureImagesAround(index: Int) {
        val pages = _uiState.value.pages
        listOfNotNull(pages.getOrNull(index), pages.getOrNull(index + 1))
            .filter { it.imageId.isNotBlank() && !_uiState.value.pageImages.containsKey(it.imageId) }
            .forEach { page ->
                viewModelScope.launch {
                    val file = storyImageRepository.imageFor(storyId, page.imageId)
                    if (file != null) {
                        _uiState.value = _uiState.value.copy(
                            pageImages = _uiState.value.pageImages + (page.imageId to file)
                        )
                    }
                }
            }
    }

    private fun loadStory() {
        viewModelScope.launch {
            val story = storyRepository.getStoryById(storyId)
            if (story != null) {
                val pages = parsePages(story.pagesJson)
                _uiState.value = _uiState.value.copy(
                    story = story,
                    pages = pages,
                    currentPageIndex = if (story.isCompleted) 0 else story.currentPage,
                    isLoading = false
                )
                ensureImagesAround(_uiState.value.currentPageIndex)
            } else {
                _uiState.value = _uiState.value.copy(error = "Story not found", isLoading = false)
            }
        }
    }

    private fun parsePages(jsonStr: String): List<StoryPage> {
        if (jsonStr.isBlank() || jsonStr == "[]") return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                StoryPage(
                    pageNumber = obj.optInt("pageNumber", i + 1),
                    kasiguranin = obj.optString("kasiguranin", ""),
                    tagalog = obj.optString("tagalog", ""),
                    english = obj.optString("english", ""),
                    audioFileName = obj.optString("audioFileName", ""),
                    illustrationDesc = obj.optString("illustrationDesc", ""),
                    imageId = obj.optString("imageId", "")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun nextPage() {
        val state = _uiState.value
        if (state.currentPageIndex < state.pages.size - 1) {
            val newIndex = state.currentPageIndex + 1
            _uiState.value = state.copy(currentPageIndex = newIndex)
            ensureImagesAround(newIndex)
            
            // Award XP for reading a page
            viewModelScope.launch {
                userProgressRepository.addXp(Constants.XP_PER_STORY_PAGE)
                if (!state.story!!.isCompleted) {
                    storyRepository.updateCurrentPage(storyId, newIndex)
                }
            }
        } else {
            completeStory()
        }
    }

    fun previousPage() {
        val state = _uiState.value
        if (state.currentPageIndex > 0) {
            val newIndex = state.currentPageIndex - 1
            _uiState.value = state.copy(currentPageIndex = newIndex)
            ensureImagesAround(newIndex)
            
            viewModelScope.launch {
                if (!state.story!!.isCompleted) {
                    storyRepository.updateCurrentPage(storyId, newIndex)
                }
            }
        }
    }

    private fun completeStory() {
        viewModelScope.launch {
            val story = _uiState.value.story ?: return@launch
            if (!story.isCompleted) {
                storyRepository.markAsCompleted(storyId)
                userProgressRepository.addXp(Constants.XP_PER_STORY_COMPLETE)
                userProgressRepository.incrementStoriesCompleted()
            }
            _uiState.value = _uiState.value.copy(isFinished = true)
        }
    }
}

data class StoryReaderUiState(
    val story: StoryEntity? = null,
    val pages: List<StoryPage> = emptyList(),
    val currentPageIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isFinished: Boolean = false,
    val vocabulary: List<VocabularyEntity> = emptyList(),
    /** Cached illustration files by page `imageId`. Absent means "no picture", which is a normal state. */
    val pageImages: Map<String, java.io.File> = emptyMap()
)
