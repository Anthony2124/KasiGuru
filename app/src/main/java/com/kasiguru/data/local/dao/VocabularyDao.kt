package com.kasiguru.data.local.dao

import androidx.room.*
import com.kasiguru.data.local.entity.VocabularyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {

    @Query("SELECT * FROM vocabulary ORDER BY category, kasiguranin")
    fun getAllVocabulary(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary")
    suspend fun getAllVocabularyOnce(): List<VocabularyEntity>

    @Query("SELECT * FROM vocabulary WHERE category = :category ORDER BY kasiguranin")
    fun getVocabularyByCategory(category: String): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE id = :id")
    suspend fun getVocabularyById(id: Int): VocabularyEntity?

    @Query("SELECT * FROM vocabulary WHERE LOWER(kasiguranin) = LOWER(:word) LIMIT 1")
    suspend fun getVocabularyByWord(word: String): VocabularyEntity?

    @Query("DELETE FROM vocabulary WHERE id NOT IN (SELECT MIN(id) FROM vocabulary GROUP BY LOWER(kasiguranin))")
    suspend fun deleteDuplicateWords()

    @Query("SELECT * FROM vocabulary WHERE isLearned = 1 ORDER BY kasiguranin")
    fun getLearnedVocabulary(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE isLearned = 0 ORDER BY RANDOM() LIMIT :count")
    suspend fun getUnlearnedWords(count: Int): List<VocabularyEntity>

    @Query("SELECT * FROM vocabulary WHERE nextReviewDate <= :todayDate OR nextReviewDate = '' ORDER BY nextReviewDate ASC, RANDOM() LIMIT :limit")
    suspend fun getDueReviewWords(todayDate: String, limit: Int = 10): List<VocabularyEntity>

    /**
     * Words with a real SM-2 review date that has arrived.
     *
     * Unlike [getDueReviewWords] this excludes `nextReviewDate = ''`. A never-reviewed word has an
     * empty date, which sorts before every real date, so the looser query treats the entire unseen
     * corpus as "due" - fine when filling a practice deck, but it makes any count shown to the
     * learner read 20 on a fresh install.
     */
    @Query("SELECT * FROM vocabulary WHERE nextReviewDate != '' AND nextReviewDate <= :todayDate ORDER BY nextReviewDate ASC LIMIT :limit")
    suspend fun getScheduledDueWords(todayDate: String, limit: Int = 20): List<VocabularyEntity>

    /**
     * Words forgotten at least [minLapses] times, hardest-hit first.
     *
     * Ordered by lapses rather than randomly: if only two can be woven into a lesson, they should be
     * the two failing most.
     */
    @Query("SELECT * FROM vocabulary WHERE lapses >= :minLapses ORDER BY lapses DESC LIMIT :limit")
    suspend fun getLeechWords(minLapses: Int, limit: Int): List<VocabularyEntity>

    @Query("SELECT * FROM vocabulary ORDER BY RANDOM() LIMIT :count")
    suspend fun getRandomWords(count: Int): List<VocabularyEntity>

    /**
     * Candidate distractors from the answer's own semantic field.
     *
     * Selected in the database rather than by sampling random rows and filtering in memory: the old
     * approach drew 40 rows at random and kept the ones that happened to share a category, so a
     * small category could silently yield nothing and quietly fall back to unrelated words.
     */
    @Query(
        "SELECT * FROM vocabulary WHERE id != :excludeId AND category = :category " +
            "ORDER BY RANDOM() LIMIT :count"
    )
    suspend fun getRandomWordsInCategory(
        category: String,
        excludeId: Int,
        count: Int
    ): List<VocabularyEntity>

    /** Candidate distractors from outside the answer's field, for a word still being introduced. */
    @Query(
        "SELECT * FROM vocabulary WHERE id != :excludeId AND category != :category " +
            "ORDER BY RANDOM() LIMIT :count"
    )
    suspend fun getRandomWordsOutsideCategory(
        category: String,
        excludeId: Int,
        count: Int
    ): List<VocabularyEntity>

    /**
     * Looks a word up by any of its written forms.
     *
     * Backs the remediation line after a wrong answer: the learner picked *something*, and saying
     * what that something actually means is the difference between being marked wrong and being
     * taught. The option they tapped may be a headword or a gloss, so all three columns are tried.
     */
    @Query(
        "SELECT * FROM vocabulary WHERE LOWER(kasiguranin) = LOWER(:text) " +
            "OR LOWER(tagalog) = LOWER(:text) OR LOWER(english) = LOWER(:text) LIMIT 1"
    )
    suspend fun getVocabularyByAnyForm(text: String): VocabularyEntity?

    @Query("SELECT * FROM vocabulary ORDER BY timesReviewed ASC, RANDOM() LIMIT :count")
    suspend fun getFreshWords(count: Int): List<VocabularyEntity>

    @Query("SELECT COUNT(*) FROM vocabulary WHERE isLearned = 1")
    fun getLearnedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vocabulary")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vocabulary")
    suspend fun getTotalCountDirect(): Int

    @Query("SELECT DISTINCT category FROM vocabulary ORDER BY category")
    fun getCategories(): Flow<List<String>>

    /**
     * Backs the floating dictionary search bar. Neither existing "search" field queried this
     * table before - VocabularyScreen's filtered only the static category list, and
     * CategoryDetailScreen's filtered an already-loaded in-memory list for one category. This is
     * the first real query against the word text itself, so it can search across every category.
     */
    @Query(
        "SELECT * FROM vocabulary WHERE kasiguranin LIKE '%' || :query || '%' " +
            "OR tagalog LIKE '%' || :query || '%' OR english LIKE '%' || :query || '%' " +
            "ORDER BY kasiguranin LIMIT :limit"
    )
    fun searchVocabulary(query: String, limit: Int = 50): Flow<List<VocabularyEntity>>

    @Update
    suspend fun updateVocabulary(vocabulary: VocabularyEntity)

    @Query("UPDATE vocabulary SET isLearned = 1, timesReviewed = timesReviewed + 1 WHERE id = :id")
    suspend fun markAsLearned(id: Int)

    @Query("UPDATE vocabulary SET isLearned = 0 WHERE id = :id")
    suspend fun unmarkAsLearned(id: Int)

    @Query("UPDATE vocabulary SET timesReviewed = timesReviewed + 1 WHERE id = :id")
    suspend fun incrementReviewCount(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vocabulary: List<VocabularyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vocabulary: VocabularyEntity)
}
