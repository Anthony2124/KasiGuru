package com.kasiguru.data.repository

import com.kasiguru.data.local.dao.LessonDao
import com.kasiguru.data.local.entity.LessonProgressEntity
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.domain.lesson.Exercise
import com.kasiguru.domain.lesson.ExerciseGenerator
import com.kasiguru.domain.lesson.LessonPlan
import com.kasiguru.domain.lesson.LessonRef
import com.kasiguru.domain.lesson.LessonUnit
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The lesson system.
 *
 * Units and lessons are *derived* from the vocabulary corpus rather than authored, so this repository
 * owns the join between the dictionary (which the admin portal edits) and the learner's progress
 * (which only the app writes). See [LessonPlan] for why the split works that way.
 */
@Singleton
class LessonRepository @Inject constructor(
    private val lessonDao: LessonDao,
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository,
    private val exerciseGenerator: ExerciseGenerator
) {

    fun observeProgress(): Flow<List<LessonProgressEntity>> = lessonDao.observeAll()

    fun observeCompletedCount(): Flow<Int> = lessonDao.observeCompletedCount()

    /**
     * Every unit with its completion state, ordered by the corpus's own category order so the
     * sequence matches what the Dictionary shows.
     */
    suspend fun units(): List<LessonUnit> {
        val wordsByCategory = allWordsByCategory()
        val completed = lessonDao.getAllOnce()
            .filter { it.isComplete }
            .groupingBy { it.unitId }
            .eachCount()

        return wordsByCategory.map { (category, words) ->
            LessonUnit(
                id = category,
                title = category,
                wordCount = words.size,
                lessonCount = LessonPlan.lessonCountFor(words.size),
                completedLessons = completed[category] ?: 0
            )
        }
    }

    /** The words belonging to one lesson, in corpus order so lessons stay stable between runs. */
    suspend fun wordsFor(ref: LessonRef): List<VocabularyEntity> {
        val words = allWordsByCategory()[ref.unitId] ?: return emptyList()
        val range = LessonPlan.wordIndicesFor(ref.lessonIndex, words.size)
        if (range.isEmpty()) return emptyList()
        return words.slice(range)
    }

    suspend fun exercisesFor(ref: LessonRef): List<Exercise> =
        exerciseGenerator.build(wordsFor(ref))

    /**
     * The next lesson to do: the first incomplete lesson in the first incomplete unit.
     *
     * Returns null only when every lesson in the corpus is complete, which the Learn screen renders
     * as a genuine finished state rather than as an empty list.
     */
    suspend fun nextLesson(): LessonRef? {
        val completed = lessonDao.getAllOnce().filter { it.isComplete }
            .map { it.unitId to it.lessonIndex }.toSet()

        allWordsByCategory().forEach { (category, words) ->
            val lessonCount = LessonPlan.lessonCountFor(words.size)
            for (index in 0 until lessonCount) {
                if (category to index !in completed) return LessonRef(category, index)
            }
        }
        return null
    }

    suspend fun progressFor(ref: LessonRef): LessonProgressEntity? =
        lessonDao.get(ref.unitId, ref.lessonIndex)

    /**
     * Records a finished lesson and awards XP.
     *
     * [accuracy] is the fraction of exercises answered correctly on the *first* attempt, so retrying
     * until correct still finishes the lesson but does not earn the perfect bonus. Returns the XP
     * awarded so the completion screen can count it up.
     *
     * Best accuracy is kept rather than last, so replaying a lesson can only improve the record.
     */
    suspend fun completeLesson(ref: LessonRef, accuracy: Float): Int {
        val existing = lessonDao.get(ref.unitId, ref.lessonIndex)
        lessonDao.upsert(
            LessonProgressEntity(
                unitId = ref.unitId,
                lessonIndex = ref.lessonIndex,
                isComplete = true,
                bestAccuracy = maxOf(existing?.bestAccuracy ?: 0f, accuracy),
                timesCompleted = (existing?.timesCompleted ?: 0) + 1,
                lastCompletedAt = System.currentTimeMillis()
            )
        )

        val xp = LessonPlan.xpFor(accuracy)
        userProgressRepository.addXp(xp)
        return xp
    }

    /** Lesson completions since [sinceEpochMillis], for the weekly strip and XP chart. */
    suspend fun completionsSince(sinceEpochMillis: Long): List<LessonProgressEntity> =
        lessonDao.completedSince(sinceEpochMillis)

    /** Merge point for cross-device sync, mirroring how game levels are reconciled. */
    suspend fun upsertAll(progress: List<LessonProgressEntity>) = lessonDao.upsertAll(progress)

    suspend fun allProgressOnce(): List<LessonProgressEntity> = lessonDao.getAllOnce()

    /**
     * Groups the corpus by category, preserving the order words come back in.
     *
     * Read fresh on each call rather than cached: the realtime Firestore listener can add words while
     * the app is open, and a stale unit list would silently hide the new content.
     */
    private suspend fun allWordsByCategory(): Map<String, List<VocabularyEntity>> =
        vocabularyRepository.getAllVocabularyOnce()
            .filter { it.category.isNotBlank() }
            .groupBy { it.category }
}
