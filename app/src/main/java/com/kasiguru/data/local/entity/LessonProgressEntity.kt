package com.kasiguru.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Per-lesson progress.
 *
 * A *unit* is one vocabulary category and a *lesson* is a slice of words from it, so lesson identity
 * is derived rather than stored: see [com.kasiguru.domain.lesson.LessonPlan]. Only the learner's
 * progress lives in the database, which means the corpus can grow through the admin portal without a
 * migration — new words simply extend the last unit.
 */
@Entity(
    tableName = "lesson_progress",
    primaryKeys = ["unitId", "lessonIndex"],
    indices = [Index(value = ["isComplete"], name = "index_lesson_progress_isComplete")]
)
data class LessonProgressEntity(
    /** Category name, used verbatim as the unit key. */
    val unitId: String = "",
    /** Zero-based index of the lesson within its unit. */
    val lessonIndex: Int = 0,
    val isComplete: Boolean = false,
    /** Best run as a 0..1 fraction of correct first attempts. */
    val bestAccuracy: Float = 0f,
    val timesCompleted: Int = 0,
    /** Epoch millis of the most recent completion; 0 when never completed. */
    val lastCompletedAt: Long = 0
)
