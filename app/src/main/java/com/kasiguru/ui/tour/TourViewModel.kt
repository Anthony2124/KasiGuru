package com.kasiguru.ui.tour

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** A stop with its destination already worked out. */
data class ResolvedStop(val stop: TourStop, val route: String)

/** A chapter in flight. */
data class ActiveTour(
    val chapter: TourChapter,
    val stops: List<ResolvedStop>,
    val index: Int,
    /** Where the learner was when the chapter began, and where finishing or skipping returns them. */
    val entryRoute: String
) {
    val current: ResolvedStop? get() = stops.getOrNull(index)
    val isLast: Boolean get() = index >= stops.lastIndex
}

/**
 * Drives the guided tour and owns the only thing about it that must survive a rotation: where the
 * learner is.
 *
 * Lives in the navigation shell for the same reason `LevelUpViewModel` does - a chapter crosses every
 * tab, so it cannot belong to any one screen. Where the anchors happen to be on screen is a separate,
 * deliberately composition-scoped concern; see [TourAnchorRegistry].
 */
@HiltViewModel
class TourViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _active = MutableStateFlow<ActiveTour?>(null)
    val active: StateFlow<ActiveTour?> = _active.asStateFlow()

    /** Checkpoint signal. Collected with a debounce so a ten-stop chapter costs one or two writes. */
    private val checkpoints = MutableStateFlow<Pair<String, Int>?>(null)

    /**
     * A chapter the learner walked away from, offered on the help page as "continue".
     *
     * Reads the repository's raw flow directly rather than sharing a single upstream collection with
     * [chapterStates]. An earlier version had chapterStates' combine() consume this same StateFlow
     * instance; when the Help screen (the only collector of either) was off screen long enough for
     * WhileSubscribed's grace period to lapse, the shared instance could restart from a frozen value
     * and be slow to pick up a write that happened while nothing was collecting - the help page kept
     * showing a chapter as in-progress after it had already been skipped, correcting itself only on
     * the next app launch. Two independent, direct collections of the same cheap DataStore read costs
     * nothing and removes that whole class of staleness.
     */
    val resumePoint: StateFlow<TourResumePoint?> = userPreferencesRepository.tourResumePoint
        .map { sanitizeResume(it?.first, it?.second) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** What the help page shows against each chapter. */
    val chapterStates: StateFlow<Map<TourChapterId, TourChapterState>> =
        combine(
            userPreferencesRepository.tourCompletedChapters,
            userPreferencesRepository.tourSkippedChapters,
            userPreferencesRepository.tourBaselineVersion,
            userPreferencesRepository.tourResumePoint.map { sanitizeResume(it?.first, it?.second) }
        ) { done, skipped, baseline, resume ->
            tourChapters.associate { chapter ->
                chapter.id to chapterState(chapter, done, skipped, baseline, resume)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        startCheckpointing()

        viewModelScope.launch {
            // Collected, not read once: this view model is built with the navigation shell, which
            // happens on Splash - before the wizard has run, let alone written the flag - so a single
            // read at construction always sees false and the core chapter never fires.
            //
            // distinctUntilChanged matters as much as the collection does: the underlying DataStore
            // emits on every preference write, so without it an unrelated key changing mid-tour would
            // re-deliver `true` and rewind the learner to stop one.
            userPreferencesRepository.tutorialPending
                .distinctUntilChanged()
                .collect { pending ->
                    if (pending && _active.value == null) {
                        // A direct suspend read, not resumePoint.value: WhileSubscribed does not start
                        // collecting the DataStore flow until something actually subscribes, and
                        // nothing has necessarily subscribed to resumePoint yet this early - the Help
                        // screen, its only other reader, may never have been opened in this process.
                        // Reading resumePoint.value here could silently see the untouched default
                        // instead of a real saved position.
                        val stored = userPreferencesRepository.tourResumePoint.first()
                        val resume = sanitizeResume(stored?.first, stored?.second)
                            ?.takeIf { it.chapterId == TourChapterId.Core }
                        startChapter(TourChapterId.Core, entryRoute = null, at = resume?.step ?: 0)
                    }
                }
        }
    }

    @OptIn(FlowPreview::class)
    private fun startCheckpointing() {
        viewModelScope.launch {
            // filterNotNull() before debounce() has a real trap: when finish()/skip() set checkpoints
            // to null to mean "stop tracking", that null itself is filtered out and never reaches
            // debounce - so a pending emission from just before the tour ended keeps its timer running
            // and writes a resume point back to disk *after* finish() already cleared it. The tour
            // then reads as both completed and mid-flight.
            //
            // The fix is not to let that stale write win: only persist if the tour it names is still
            // the one actually running. A finished or skipped tour has already set _active to null, so
            // the id comparison below fails and the write is dropped.
            checkpoints.filterNotNull().debounce(CHECKPOINT_DEBOUNCE_MS).collect { (chapterId, step) ->
                if (_active.value?.chapter?.id?.name == chapterId) {
                    userPreferencesRepository.setTourResumePoint(chapterId, step)
                }
            }
        }
    }

    /**
     * Begins a chapter.
     *
     * @param entryRoute where the learner is now. Finishing or skipping returns them here, so a
     *   chapter launched from the help page ends back on the help page rather than wherever its last
     *   stop happened to leave them.
     */
    fun startChapter(id: TourChapterId, entryRoute: String?, at: Int = 0) {
        val chapter = chapterById(id) ?: return
        val stops = chapter.stops.mapNotNull { stop ->
            (stop.target as? TourTarget.Fixed)?.let { ResolvedStop(stop, it.route) }
        }
        if (stops.isEmpty()) return

        _active.value = ActiveTour(
            chapter = chapter,
            stops = stops,
            index = at.coerceIn(0, stops.lastIndex),
            entryRoute = entryRoute ?: stops.first().route
        )
        checkpoints.value = id.name to at
    }

    fun next() {
        val tour = _active.value ?: return
        if (tour.isLast) finish() else move(tour.index + 1)
    }

    fun back() {
        val tour = _active.value ?: return
        if (tour.index == 0) skip() else move(tour.index - 1)
    }

    private fun move(index: Int) {
        val tour = _active.value ?: return
        _active.value = tour.copy(index = index)
        checkpoints.value = tour.chapter.id.name to index
    }

    /** Reached the end. Recorded at the chapter's current version, so a later revision re-offers it. */
    fun finish() {
        val tour = _active.value ?: return
        _active.value = null
        checkpoints.value = null
        persist {
            userPreferencesRepository.markTourChapterCompleted(stamp(tour.chapter))
            if (tour.chapter.id == TourChapterId.Core) {
                userPreferencesRepository.setTutorialPending(false)
            }
        }
    }

    /** Left early. Offered again later, but never badged - they already said no once. */
    fun skip() {
        val tour = _active.value ?: return
        _active.value = null
        checkpoints.value = null
        persist {
            userPreferencesRepository.markTourChapterSkipped(tour.chapter.id.name)
            if (tour.chapter.id == TourChapterId.Core) {
                userPreferencesRepository.setTutorialPending(false)
            }
        }
    }

    /**
     * Flushes the in-flight position immediately.
     *
     * Called when the app is backgrounded. The debounce is the belt to this brace: a crash or a
     * low-memory kill that never delivers ON_STOP still resumes within a second and a half of the
     * last tap.
     */
    fun checkpoint() {
        val tour = _active.value ?: return
        persist { userPreferencesRepository.setTourResumePoint(tour.chapter.id.name, tour.index) }
    }

    /** Replays the core chapter from the top, for the Settings row. */
    fun restart() {
        persist {
            userPreferencesRepository.clearTourResumePoint()
            userPreferencesRepository.setTutorialPending(true)
        }
        startChapter(TourChapterId.Core, entryRoute = null)
    }

    /**
     * NonCancellable because these writes are the record of what the learner did, and the scope they
     * run in can be torn down by the very navigation that finishing a chapter triggers - the same trap
     * that made `OnboardingViewModel` silently lose its third write until it was wrapped the same way.
     */
    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch { withContext(NonCancellable) { block() } }
    }

    private companion object {
        const val CHECKPOINT_DEBOUNCE_MS = 1_500L
    }
}
