package com.kasiguru.data.repository

import com.kasiguru.data.local.dao.VocabularyDao
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.util.Constants
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

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

    suspend fun markAsLearned(id: Int) {
        vocabularyDao.markAsLearned(id)
        userProgressRepository.addXp(Constants.XP_PER_WORD_LEARNED)
        userProgressRepository.incrementWordsLearned()
    }

    suspend fun unmarkAsLearned(id: Int) {
        vocabularyDao.unmarkAsLearned(id)
    }

    suspend fun incrementReviewCount(id: Int) =
        vocabularyDao.incrementReviewCount(id)
}
