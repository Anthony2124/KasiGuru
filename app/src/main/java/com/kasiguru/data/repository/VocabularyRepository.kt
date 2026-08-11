package com.kasiguru.data.repository

import com.kasiguru.data.local.dao.VocabularyDao
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.util.Constants
import com.kasiguru.util.srs.ReviewRating
import com.kasiguru.util.srs.Sm2Algorithm
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class VocabularyRepository @Inject constructor(
    private val vocabularyDao: VocabularyDao,
    private val userProgressRepository: UserProgressRepository
) {
    fun getAllVocabulary(): Flow<List<VocabularyEntity>> =
        vocabularyDao.getAllVocabulary()

    fun getVocabularyByCategory(category: String): Flow<List<VocabularyEntity>> =
        vocabularyDao.getVocabularyByCategory(category)

    suspend fun getVocabularyById(id: Int): VocabularyEntity? =
        vocabularyDao.getVocabularyById(id)

    fun getLearnedVocabulary(): Flow<List<VocabularyEntity>> =
        vocabularyDao.getLearnedVocabulary()

    suspend fun getRandomWords(count: Int): List<VocabularyEntity> =
        vocabularyDao.getRandomWords(count)

    suspend fun getFreshWords(count: Int): List<VocabularyEntity> =
        vocabularyDao.getFreshWords(count)

    suspend fun getUnlearnedWords(count: Int): List<VocabularyEntity> =
        vocabularyDao.getUnlearnedWords(count)

    suspend fun getDueReviewWords(count: Int = 10): List<VocabularyEntity> {
        val today = LocalDate.now().toString()
        val due = vocabularyDao.getDueReviewWords(today, count)
        return if (due.isNotEmpty()) due else vocabularyDao.getRandomWords(count)
    }

    fun getLearnedCount(): Flow<Int> =
        vocabularyDao.getLearnedCount()

    fun getTotalCount(): Flow<Int> =
        vocabularyDao.getTotalCount()

    suspend fun getTotalCountDirect(): Int =
        vocabularyDao.getTotalCountDirect()

    suspend fun insertAll(words: List<VocabularyEntity>) =
        vocabularyDao.insertAll(words)

    suspend fun updateVocabulary(word: VocabularyEntity) =
        vocabularyDao.updateVocabulary(word)

    fun getCategories(): Flow<List<String>> =
        vocabularyDao.getCategories()

    /**
     * Processes a spaced repetition review for a word using SM-2.
     * Updates SM-2 fields and increments user progress stats if learned threshold is passed.
     */
    suspend fun processWordReview(word: VocabularyEntity, rating: ReviewRating): VocabularyEntity {
        val sm2Result = Sm2Algorithm.calculateNextReview(word, rating)
        val updatedWord = word.copy(
            easinessFactor = sm2Result.easinessFactor,
            intervalDays = sm2Result.intervalDays,
            nextReviewDate = sm2Result.nextReviewDate,
            timesReviewed = sm2Result.timesReviewed,
            isLearned = sm2Result.isLearned
        )
        vocabularyDao.updateVocabulary(updatedWord)

        if (!word.isLearned && sm2Result.isLearned) {
            userProgressRepository.incrementWordsLearned()
            userProgressRepository.addXp(Constants.XP_PER_WORD_LEARNED)
        }
        return updatedWord
    }

    /**
     * Manually mark a word as learned from the dictionary.
     * Bypasses the multiple spaced reviews requirement for instant UI feedback.
     */
    suspend fun markAsLearned(id: Int) {
        val word = vocabularyDao.getVocabularyById(id) ?: return
        if (word.isLearned) return
        
        val sm2Result = Sm2Algorithm.calculateNextReview(word, ReviewRating.GOOD)
        val updatedWord = word.copy(
            easinessFactor = sm2Result.easinessFactor,
            intervalDays = sm2Result.intervalDays,
            nextReviewDate = sm2Result.nextReviewDate,
            timesReviewed = maxOf(2, sm2Result.timesReviewed), // Ensure SM-2 treats it as learned
            isLearned = true
        )
        vocabularyDao.updateVocabulary(updatedWord)
        
        userProgressRepository.incrementWordsLearned()
        userProgressRepository.addXp(Constants.XP_PER_WORD_LEARNED)
    }

    suspend fun unmarkAsLearned(id: Int) {
        val word = vocabularyDao.getVocabularyById(id) ?: return
        val resetWord = word.copy(
            isLearned = false,
            timesReviewed = 0,
            intervalDays = 0,
            nextReviewDate = ""
        )
        vocabularyDao.updateVocabulary(resetWord)
    }

    suspend fun incrementReviewCount(id: Int) =
        vocabularyDao.incrementReviewCount(id)

    /**
     * Fetches confusable distractors preferred in priority order:
     * 1. Same category
     * 2. Similar length / starting letter
     * 3. Fallback random
     */
    suspend fun getDistractorsForWord(targetWord: VocabularyEntity, count: Int = 3): List<VocabularyEntity> {
        val categoryWords = vocabularyDao.getRandomWords(40)
            .filter { it.id != targetWord.id && it.category.equals(targetWord.category, ignoreCase = true) }

        if (categoryWords.size >= count) {
            return categoryWords.shuffled().take(count)
        }

        val result = categoryWords.toMutableList()
        val allOtherWords = vocabularyDao.getRandomWords(50)
            .filter { it.id != targetWord.id && it.id !in result.map { r -> r.id } }

        val similarWords = allOtherWords.filter {
            abs(it.kasiguranin.length - targetWord.kasiguranin.length) <= 2 ||
                    it.kasiguranin.firstOrNull()?.lowercaseChar() == targetWord.kasiguranin.firstOrNull()?.lowercaseChar()
        }.shuffled()

        for (word in similarWords) {
            if (result.size >= count) break
            result.add(word)
        }

        if (result.size < count) {
            val remaining = allOtherWords.filter { it.id !in result.map { r -> r.id } }.shuffled()
            for (word in remaining) {
                if (result.size >= count) break
                result.add(word)
            }
        }

        return result.take(count)
    }
}
