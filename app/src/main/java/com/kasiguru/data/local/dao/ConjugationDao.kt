package com.kasiguru.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kasiguru.data.local.entity.ConjugationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConjugationDao {
    @Query("SELECT * FROM conjugations WHERE vocabulary_id = :vocabularyId")
    fun getConjugationsForWord(vocabularyId: Int): Flow<List<ConjugationEntity>>

    @Query("SELECT * FROM conjugations WHERE vocabulary_id = :vocabularyId")
    suspend fun getConjugationsForWordDirect(vocabularyId: Int): List<ConjugationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConjugation(conjugation: ConjugationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(conjugations: List<ConjugationEntity>)

    @Query("DELETE FROM conjugations WHERE vocabulary_id = :vocabularyId")
    suspend fun deleteConjugationsForWord(vocabularyId: Int)
}
