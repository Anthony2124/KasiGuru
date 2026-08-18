package com.kasiguru.ui.screens.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.BuildConfig
import com.kasiguru.data.local.entity.UserProgressEntity
import com.kasiguru.data.remote.model.AppReleaseDto
import com.kasiguru.data.repository.AppUpdateRepository
import com.kasiguru.data.repository.AuthRepository
import com.kasiguru.data.repository.GameLevelRepository
import com.kasiguru.data.repository.LessonRepository
import com.kasiguru.data.repository.StoryRepository
import com.kasiguru.data.repository.UserPreferencesRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.domain.lesson.LessonRef
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

private const val BACKUP_PROMPT_MIN_XP = 150

/** What kind of activity a Today's Path row is, which decides its colour and destination. */
enum class ActivityKind { Lesson, Review, Game, Story }

data class PathActivity(
    val kind: ActivityKind,
    val title: String,
    val subtitle: String,
    val isDone: Boolean,
    val lessonRef: LessonRef? = null
)

/** One of the four things a KasiGuru learner can actually make measurable progress in. */
data class Skill(val name: String, val percent: Int, val kind: ActivityKind)

data class DayActivity(val label: String, val dayOfMonth: Int, val practised: Boolean, val isToday: Boolean)

data class LearnUiState(
    val isLoading: Boolean = true,
    val progress: UserProgressEntity = UserProgressEntity(),
    val week: List<DayActivity> = emptyList(),
    val activities: List<PathActivity> = emptyList(),
    val skills: List<Skill> = emptyList(),
    val updateRelease: AppReleaseDto? = null,
    val showBackupPrompt: Boolean = false
) {
    /**
     * XP earned today, read from the stored ledger.
     *
     * The stored counter belongs to `dailyXpDate`; if that is not today the learner has simply not
     * earned anything yet today, so it reads as zero rather than showing yesterday's total.
     */
    val dailyXpEarned: Int
        get() = if (progress.dailyXpDate == LocalDate.now().toString()) progress.dailyXpEarned else 0

    val dailyGoalFraction: Float
        get() = if (progress.dailyGoalXp <= 0) 0f
        else (dailyXpEarned.toFloat() / progress.dailyGoalXp).coerceIn(0f, 1f)

    val dailyGoalMet: Boolean get() = dailyXpEarned >= progress.dailyGoalXp

    /** The first activity not yet done — the one the FAB and the raised card point at. */
    val currentActivity: PathActivity? get() = activities.firstOrNull { !it.isDone }
}

@HiltViewModel
class LearnViewModel @Inject constructor(
    private val userProgressRepository: UserProgressRepository,
    private val vocabularyRepository: VocabularyRepository,
    private val lessonRepository: LessonRepository,
    private val storyRepository: StoryRepository,
    private val gameLevelRepository: GameLevelRepository,
    private val appUpdateRepository: AppUpdateRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearnUiState())
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()

    init {
        observeProgress()
        refreshPlan()
        checkForUpdate()
        observeAccountState()
        viewModelScope.launch { userProgressRepository.updateStreak() }
    }

    /** Re-derives Today's Path. Called on entry and after returning from a lesson. */
    fun refreshPlan() {
        viewModelScope.launch {
            val progress = userProgressRepository.getUserProgressOnce() ?: UserProgressEntity()
            _uiState.update {
                it.copy(
                    progress = progress,
                    week = buildWeek(progress),
                    activities = buildActivities(),
                    skills = buildSkills(progress),
                    isLoading = false
                )
            }
        }
    }

    private fun observeProgress() {
        viewModelScope.launch {
            userProgressRepository.getUserProgress().collect { progress ->
                if (progress != null) {
                    _uiState.update {
                        it.copy(progress = progress, week = buildWeek(progress), isLoading = false)
                    }
                }
            }
        }
    }

    /**
     * The seven-day strip.
     *
     * The app stores only `currentStreak` and `lastActiveDate`, not a day-by-day history, so the week
     * is derived: the streak is a run of consecutive practised days ending on `lastActiveDate`. That
     * is exactly what the streak means, so the strip is accurate rather than decorative — but it
     * cannot show practice from before a broken streak, and deliberately does not pretend to.
     */
    private fun buildWeek(progress: UserProgressEntity): List<DayActivity> {
        val today = LocalDate.now()
        val lastActive = runCatching { LocalDate.parse(progress.lastActiveDate) }.getOrNull()

        return (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val practised = lastActive != null && progress.currentStreak > 0 && run {
                val daysBeforeLastActive = java.time.temporal.ChronoUnit.DAYS.between(date, lastActive)
                daysBeforeLastActive in 0 until progress.currentStreak.toLong()
            }
            DayActivity(
                label = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                dayOfMonth = date.dayOfMonth,
                practised = practised,
                isToday = date == today
            )
        }
    }

    private suspend fun buildActivities(): List<PathActivity> {
        val activities = mutableListOf<PathActivity>()

        // 1. The next lesson.
        val nextLesson = lessonRepository.nextLesson()
        if (nextLesson != null) {
            val words = lessonRepository.wordsFor(nextLesson)
            activities += PathActivity(
                kind = ActivityKind.Lesson,
                title = nextLesson.unitId,
                subtitle = "Lesson ${nextLesson.lessonIndex + 1} · ${words.size} words",
                isDone = false,
                lessonRef = nextLesson
            )
        } else {
            activities += PathActivity(
                kind = ActivityKind.Lesson,
                title = "Every lesson complete",
                subtitle = "Keep them sharp with review",
                isDone = true
            )
        }

        // 2. Spaced-repetition review, only counted as work when something is actually due.
        val due = vocabularyRepository.getDueReviewWordsStrict(limit = 20)
        activities += PathActivity(
            kind = ActivityKind.Review,
            title = "Review",
            subtitle = if (due.isEmpty()) "Nothing due today" else "${due.size} words due",
            isDone = due.isEmpty()
        )

        // 3. A game.
        activities += PathActivity(
            kind = ActivityKind.Game,
            title = "Practice game",
            subtitle = "Earn stars and XP",
            isDone = false
        )

        // 4. A story, when one is unlocked.
        val stories = storyRepository.getUnlockedStories().first()
        if (stories.isNotEmpty()) {
            val unread = stories.firstOrNull { !it.isCompleted }
            activities += PathActivity(
                kind = ActivityKind.Story,
                title = unread?.title ?: "Folk tales",
                subtitle = if (unread == null) "All stories read" else "Read and listen",
                isDone = unread == null
            )
        }

        return activities
    }

    /**
     * Skill percentages, each backed by a number the app really tracks.
     *
     * The reference designs show Reading / Listening / Speaking / Conversation, but KasiGuru cannot
     * measure speaking, so inventing that tile would be a lie drawn on a dashboard. These four map to
     * data that exists.
     */
    private suspend fun buildSkills(progress: UserProgressEntity): List<Skill> {
        val allWords = vocabularyRepository.getAllVocabularyOnce()
        val learned = allWords.count { it.isLearned }
        val vocabularyPercent = percent(learned, allWords.size)

        val units = lessonRepository.units()
        val lessonPercent = percent(
            units.sumOf { it.completedLessons },
            units.sumOf { it.lessonCount }
        )

        val stars = gameLevelRepository.getTotalStars()
        // Six games x 30 levels x 3 stars is the ceiling the level seeder creates.
        val gamePercent = percent(stars, 6 * 30 * 3)

        val stories = storyRepository.getAllStories().first()
        val storyPercent = percent(stories.count { it.isCompleted }, stories.size)

        return listOf(
            Skill("Vocabulary", vocabularyPercent, ActivityKind.Lesson),
            Skill("Lessons", lessonPercent, ActivityKind.Review),
            Skill("Games", gamePercent, ActivityKind.Game),
            Skill("Stories", storyPercent, ActivityKind.Story)
        )
    }

    private fun percent(part: Int, whole: Int): Int =
        if (whole <= 0) 0 else ((part.toFloat() / whole) * 100).toInt().coerceIn(0, 100)

    private fun checkForUpdate() {
        viewModelScope.launch {
            val latest = appUpdateRepository.getLatestRelease().getOrNull() ?: return@launch
            if (latest.versionCode <= BuildConfig.VERSION_CODE) return@launch
            val dismissed = userPreferencesRepository.dismissedUpdateVersion.first()
            if (!latest.forceUpdate && latest.versionCode <= dismissed) return@launch
            _uiState.update { it.copy(updateRelease = latest) }
        }
    }

    fun dismissUpdate() {
        val dismissedVersion = _uiState.value.updateRelease?.versionCode
        _uiState.update { it.copy(updateRelease = null) }
        if (dismissedVersion != null) {
            viewModelScope.launch {
                userPreferencesRepository.setDismissedUpdateVersion(dismissedVersion)
            }
        }
    }

    fun dismissBackupPrompt() {
        _uiState.update { it.copy(showBackupPrompt = false) }
        viewModelScope.launch { userPreferencesRepository.setBackupPromptDismissed(true) }
    }

    private fun observeAccountState() {
        viewModelScope.launch {
            combine(
                authRepository.accountState,
                userProgressRepository.getUserProgress(),
                userPreferencesRepository.backupPromptDismissed
            ) { account, progress, dismissed ->
                !dismissed &&
                    account.isSignedIn &&
                    !account.isRecoverable &&
                    (progress?.totalXp ?: 0) >= BACKUP_PROMPT_MIN_XP
            }.collect { show ->
                _uiState.update { it.copy(showBackupPrompt = show) }
            }
        }
    }
}
