package com.kasiguru.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Normalized entity for specific verb conjugations linked to a parent vocabulary word.
 * Foreign key link with ON DELETE CASCADE ensures orphaned conjugations are automatically removed.
 */
@Entity(
    tableName = "conjugations",
    foreignKeys = [
        ForeignKey(
            entity = VocabularyEntity::class,
            parentColumns = ["id"],
            childColumns = ["vocabulary_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["vocabulary_id"])]
)
data class ConjugationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "vocabulary_id")
    val vocabularyId: Int,
    @ColumnInfo(name = "conjugated_form")
    val conjugatedForm: String,
    val tense: String, // 'perfective', 'imperfective', 'contemplative'
    @ColumnInfo(name = "affix_type")
    val affixType: String? = null
)
