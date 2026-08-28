package com.kasiguru.ui.screens.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.BuildConfig
import com.kasiguru.data.local.entity.StoryEntity
import com.kasiguru.data.local.entity.UserProgressEntity
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.remote.model.AnnouncementDto
import com.kasiguru.data.remote.model.AppReleaseDto
import com.kasiguru.data.local.entity.MetricType
import com.kasiguru.data.repository.AnnouncementRepository
import com.kasiguru.data.repository.AppUpdateRepository
import com.kasiguru.data.repository.AuthRepository
import com.kasiguru.data.repository.SubmissionRepository
import com.kasiguru.data.repository.GameLevelRepository
import com.kasiguru.data.repository.LessonRepository
import com.kasiguru.data.repository.StoryRepository
import com.kasiguru.data.repository.UserPreferencesRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.domain.lesson.LessonRef
import dagger.hilt.android.lifecycle.HiltViewModel
import com.kasiguru.util.toIsoString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

/**
 * "1 word" or "5 words".
 *
 * The count is read aloud by TalkBack from the goal ring's description and printed on the review
 * card, so "1 words due" is not a small thing: it is the first line of the app most learners see
 * every morning.
 */
internal fun wordsToReview(count: Int): String = if (count == 1) "1 word" else "$count words"

data class LearnUiState(
    val isLoading: Boolean = true,
    val progress: UserProgressEntity = UserProgressEntity(),
    val week: List<DayActivity> = emptyList(),
    val activities: List<PathActivity> = emptyList(),
    val skills: List<Skill> = emptyList(),
    /** Every story, locked ones included - the lock is the motivation, so the shelf shows them. */
    val stories: List<StoryEntity> = emptyList(),
    val updateRelease: AppReleaseDto? = null,
    val showBackupPrompt: Boolean = false,
    val announcements: List<AnnouncementDto> = emptyList(),
    /** Words still due for review right now. Part of the day goal, not just a number on a card. */
    val wordsDue: Int = 0,
    val streakQuota: com.kasiguru.data.repository.DailyStreakQuota = com.kasiguru.data.repository.DailyStreakQuota()
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

    /**
     * The day is done when the XP target is met **and** nothing is still due.
     *
     * XP alone was gameable against the learner: reading two stories fills the ring while every word
     * scheduled for today goes unreviewed, so the app would report a met goal on precisely the day
     * retention was being lost. Reviews are the part of a day that spaced repetition actually needs,
     * so they belong in the target.
     */
    val dailyGoalMet: Boolean
        get() = dailyXpEarned >= progress.dailyGoalXp && wordsDue == 0

    /** What is left of the day goal, for the ring description. */
    val dailyGoalRemainder: String
        get() = when {
            dailyGoalMet -> "goal met"
            dailyXpEarned < progress.dailyGoalXp && wordsDue > 0 ->
                "${progress.dailyGoalXp - dailyXpEarned} XP to go and ${wordsToReview(wordsDue)} to review"
            dailyXpEarned < progress.dailyGoalXp -> "${progress.dailyGoalXp - dailyXpEarned} XP to go"
            else -> "${wordsToReview(wordsDue)} still to review"
        }

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
    private val authRepository: AuthRepository,
    private val announcementRepository: AnnouncementRepository,
    private val submissionRepository: SubmissionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearnUiState())
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()

    init {
        observeProgress()
        observeStreakQuota()
        refreshPlan()
        checkForUpdate()
        observeAccountState()
        observeAnnouncements()
        checkSubmissionAchievements()
        observeAuthForRefresh()
        // No streak call here on purpose. Opening this screen is not learning; the streak now
        // advances from answered reviews, finished lessons and finished games instead.
    }

    private fun observeStreakQuota() {
        viewModelScope.launch {
            val today = LocalDate.now().toIsoString()
            userProgressRepository.getDailyStreakQuota(today).collect { quota ->
                _uiState.update { it.copy(streakQuota = quota) }
            }
        }
    }

    /**
     * Backs "Trusted Voice" / "Corpus Builder": the app has no push notification for "your
     * submission was approved," so this checks on every Learn open instead, which is frequent
     * enough that the badge unlocks promptly without needing new sync infrastructure.
     */
    private fun checkSubmissionAchievements() {
        viewModelScope.launch {
            submissionRepository.getApprovedSubmissionCount().onSuccess { count ->
                userProgressRepository.checkAchievements(MetricType.SUBMISSIONS_APPROVED, count)
            }
        }
    }

    private fun observeAnnouncements() {
        viewModelScope.launch {
            announcementRepository.getAnnouncements().collect { list ->
                _uiState.update { it.copy(announcements = list) }
            }
        }
    }

    /** Re-derives Today's Path. Called on entry and after returning from a lesson. */
    fun refreshPlan() {
        viewModelScope.launch {
            val progress = userProgressRepository.getUserProgressOnce() ?: UserProgressEntity()
            val due = vocabularyRepository.getDueReviewWordsStrict(limit = 20)
            _uiState.update {
                it.copy(
                    progress = progress,
                    week = buildWeek(progress),
                    activities = buildActivities(due),
                    skills = buildSkills(progress),
                    stories = storyRepository.getAllStories().first(),
                    wordsDue = due.size,
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

    private suspend fun buildActivities(due: List<VocabularyEntity>): List<PathActivity> {
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
        //
        // Placed ahead of the lesson when anything is due, because currentActivity -- the raised
        // card and the FAB both point at it -- is simply the first item not yet done. With the
        // lesson always first, the app spent every session recommending new words while the ones
        // the learner is about to forget sat behind them. Due material is the more urgent work by
        // definition: that is what a review date means.
        val review = PathActivity(
            kind = ActivityKind.Review,
            title = "Review",
            subtitle = if (due.isEmpty()) "Nothing due today" else "${wordsToReview(due.size)} due",
            isDone = due.isEmpty()
        )
        if (due.isEmpty()) activities += review else activities.add(0, review)

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

        return listOf(
            Skill("Vocabulary", vocabularyPercent, ActivityKind.Lesson),
            Skill("Lessons", lessonPercent, ActivityKind.Review),
            Skill("Games", gamePercent, ActivityKind.Game)
            // Stories used to be a fourth tile here. The shelf below Today's Path carries them with
            // covers, page counts and lock state, which is strictly more than a percentage.
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

    /**
     * Re-derives Today's Path whenever the signed-in account changes.
     *
     * After sign-out the UID flips from the old account to a new anonymous one.
     * Without this, the activities list, wordsDue count, and skill percentages stay
     * stale because refreshPlan() was a one-shot in init. drop(1) avoids doubling
     * the initial refresh that init already performs.
     */
    private fun observeAuthForRefresh() {
        viewModelScope.launch {
            authRepository.accountState
                .map { it.uid }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    // Immediately clear stale review/activity state so the UI shows a
                    // clean slate while refreshPlan() re-derives from the database.
                    // Without this, the old "review done" / wordsDue count stays visible
                    // for the entire duration of the async refresh after a sign-out.
                    _uiState.update {
                        it.copy(
                            wordsDue = 0,
                            activities = emptyList(),
                            skills = emptyList(),
                            streakQuota = com.kasiguru.data.repository.DailyStreakQuota(),
                            isLoading = true
                        )
                    }
                    refreshPlan()
                }
        }
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
