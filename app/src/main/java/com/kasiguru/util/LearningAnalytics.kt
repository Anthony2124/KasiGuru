package com.kasiguru.util

import android.os.Bundle
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

/**
 * The events the thesis evaluation needs, and nothing else.
 *
 * Crashlytics already reports when the app breaks. Nothing reported whether anyone *learned*, which
 * makes "we shipped it" the only claim the paper can support. These events are what turn that into
 * "N learners reached stage 2, and their mean lesson accuracy was X" — so they exist for the
 * evaluation chapter, not for growth metrics.
 *
 * **No personal data leaves the device through here.** Every parameter is a count, a ratio, a
 * duration or an internal id. Never add a word a learner typed, a display name, an email or a
 * free-text field: the audience includes minors, participation is under research consent, and an
 * analytics payload is the easiest place for identifying data to escape unnoticed.
 *
 * Calls are deliberately fire-and-forget and swallow their own failures. A learner mid-lesson must
 * never see a crash because a measurement call failed, and unit tests construct ViewModels without a
 * Firebase app at all — an uninitialised SDK throws on first access, so the guard is load-bearing
 * rather than defensive habit.
 */
object LearningAnalytics {

    /** A lesson finished. [accuracy] is 0..1 across first attempts, matching the mastery rule. */
    fun lessonCompleted(stageId: String, accuracy: Float, exercises: Int, xpAwarded: Int) {
        log("lesson_completed") {
            putString("stage_id", stageId)
            // Firebase reports on integers far more usefully than floats, and whole percent is finer
            // than the evaluation needs anyway.
            putLong("accuracy_pct", (accuracy * 100).toLong().coerceIn(0, 100))
            putLong("exercise_count", exercises.toLong())
            putLong("xp_awarded", xpAwarded.toLong())
        }
    }

    /** A daily review session finished. The SM-2 claim in the paper rests on these. */
    fun reviewCompleted(dueCount: Int, gradedCount: Int) {
        log("review_completed") {
            putLong("due_count", dueCount.toLong())
            putLong("graded_count", gradedCount.toLong())
        }
    }

    /** A story was opened. [pageCount] separates a glance from a read when paired with the finish. */
    fun storyOpened(storyId: Int, pageCount: Int) {
        log("story_opened") {
            putLong("story_id", storyId.toLong())
            putLong("page_count", pageCount.toLong())
        }
    }

    /** The learner reached the last page of a story. */
    fun storyFinished(storyId: Int) {
        log("story_finished") { putLong("story_id", storyId.toLong()) }
    }

    /** A mini-game ended. [gameId] is the screen's own identifier, not a display name. */
    fun gameFinished(gameId: String, score: Int, total: Int) {
        log("game_finished") {
            putString("game_id", gameId)
            putLong("score", score.toLong())
            putLong("total", total.toLong())
        }
    }

    /** The learner levelled up — the coarsest progression signal, and the easiest to report. */
    fun levelReached(level: Int) {
        log("level_reached") { putLong("level", level.toLong()) }
    }

    private inline fun log(event: String, params: Bundle.() -> Unit) {
        try {
            Firebase.analytics.logEvent(event, Bundle().apply(params))
        } catch (_: Throwable) {
            // Measurement is never worth a crash. See the class comment.
        }
    }
}
