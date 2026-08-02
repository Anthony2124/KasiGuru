package com.kasiguru.data.repository

import com.kasiguru.data.local.dao.VocabularyDao
import com.kasiguru.data.local.entity.VocabularyEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VocabularyRepository @Inject constructor(
    private val vocabularyDao: VocabularyDao
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

    fun getLearnedCount(): Flow<Int> =
        vocabularyDao.getLearnedCount()

    fun getTotalCount(): Flow<Int> =
        vocabularyDao.getTotalCount()

    suspend fun getTotalCountDirect(): Int =
        vocabularyDao.getTotalCountDirect()

    suspend fun insertAll(words: List<VocabularyEntity>) =
        vocabularyDao.insertAll(words)

    fun getCategories(): Flow<List<String>> =
        vocabularyDao.getCategories()

    suspend fun markAsLearned(id: Int) =
        vocabularyDao.markAsLearned(id)

    suspend fun incrementReviewCount(id: Int) =
        vocabularyDao.incrementReviewCount(id)
}
