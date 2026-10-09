package com.kasiguru.domain.games

import com.kasiguru.data.local.entity.VocabularyEntity

/**
 * The hints of the games whose rounds are about more than one word: a word's simple meaning
 * beside it, one per line. Null when no line has a meaning, so the hint button stays hidden instead
 * of opening onto nothing.
 */

/** Word Search: each word still to find, with its meaning (`hintFor`'s text, or null when unwritten). */
internal fun wordSearchHint(words: List<Pair<String, String?>>): String? =
    words.mapNotNull { (word, meaning) ->
        meaning?.lineSequence()?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }?.let { "$word — $it" }
    }.joinToString("\n").ifBlank { null }

/**
 * Word Wheel: the meaning of each word still hidden on the board, with its length, but never the
 * word itself, which is the answer. [words] pairs a word's letter count with its meaning.
 */
internal fun wordWheelHint(words: List<Pair<Int, String?>>): String? =
    words.mapNotNull { (letters, meaning) ->
        meaning?.lineSequence()?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }?.let { "$letters letters — $it" }
    }.joinToString("\n").ifBlank { null }

/**
 * Sentence Order: what each word of the sentence means, in the order the sentence uses them. A
 * token is looked up as written and without its punctuation; words the dictionary does not hold are
 * left out rather than guessed.
 */
internal fun sentenceHint(tokens: List<String>, lookup: (String) -> VocabularyEntity?): String? =
    tokens.mapNotNull { raw ->
        val token = sentenceToken(raw)
        if (token.isEmpty()) return@mapNotNull null
        val entry = lookup(token) ?: return@mapNotNull null
        val meaning = tagalogGloss(entry).ifBlank { entry.english.trim() }
        meaning.takeIf { it.isNotEmpty() }?.let { "${raw.trim().trim { c -> !c.isLetter() && c != '-' }} — $it" }
    }.distinct().joinToString("\n").ifBlank { null }

/** A sentence word as the dictionary spells it: lower case, punctuation dropped, letters (ë too) and hyphens kept. */
internal fun sentenceToken(raw: String): String =
    raw.trim().lowercase().filter { it.isLetter() || it == '-' }.trim('-')
