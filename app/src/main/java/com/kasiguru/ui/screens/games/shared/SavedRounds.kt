package com.kasiguru.ui.screens.games.shared

import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.domain.games.SavedAnswer
import com.kasiguru.domain.games.SavedRound
import com.kasiguru.ui.components.GameReviewItem

/*
 * What the question games share for saving a round and picking it up again; see SavedRound.
 */

fun GameReviewItem.toSavedAnswer() = SavedAnswer(prompt, userAnswer, correctAnswer, isCorrect, subPrompt)

fun SavedAnswer.toReviewItem() = GameReviewItem(prompt, userAnswer, correctAnswer, isCorrect, subPrompt)

/**
 * The dictionary words [saved] was dealt, in order, or null when any has since left the dictionary:
 * the round can then not be rebuilt, and the game deals a fresh one.
 */
suspend fun VocabularyRepository.wordsOf(saved: SavedRound): List<VocabularyEntity>? =
    saved.questionKeys.map { key -> key.toIntOrNull()?.let { getVocabularyById(it) } ?: return null }
