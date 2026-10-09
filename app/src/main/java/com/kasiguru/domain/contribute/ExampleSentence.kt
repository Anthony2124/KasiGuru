package com.kasiguru.domain.contribute

import com.kasiguru.data.local.entity.VocabularyEntity
import java.text.Normalizer

/**
 * Whether a learner's example sentence for [word] is fit to send for review, and why not.
 *
 * The checks are the ones the sentence's readers need, nothing more: the lessons and Sentence Order
 * refuse a sentence under [MIN_WORDS] words (SentenceBank.MIN_WORDS), every reader shows the English
 * translation as the prompt, and Fill in the Blank blanks out the word, so it has to be in there. A
 * verb rarely appears bare, so any of its recorded forms counts, and so does a token built on the
 * headword (an affixed form). Kasiguranin is not judged here; a verifier does that.
 */
fun exampleSentenceProblem(word: VocabularyEntity, sentence: String, translation: String): String? {
    val tokens = sentence.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        tokens.size < MIN_WORDS -> "Write a sentence of at least $MIN_WORDS words."
        sentence.trim().length > MAX_LENGTH -> "Keep the sentence under $MAX_LENGTH characters."
        !usesWord(word, tokens) -> "Use \"${word.kasiguranin}\" (or one of its forms) in the sentence."
        translation.trim().length < 2 -> "Add what the sentence means in English."
        translation.trim().length > MAX_LENGTH -> "Keep the translation under $MAX_LENGTH characters."
        else -> null
    }
}

const val MIN_WORDS = 3
const val MAX_LENGTH = 300

private fun usesWord(word: VocabularyEntity, tokens: List<String>): Boolean {
    val forms = listOf(
        word.kasiguranin, word.rootForm, word.neutralForm,
        word.imperfectiveForm, word.perfectiveForm, word.contemplativeForm
    ).map(::plain).filter { it.length >= 2 }.toSet()
    val head = plain(word.kasiguranin)
    return tokens.map(::plain).any { token ->
        token in forms || (head.length >= 3 && token.contains(head))
    }
}

/** Lower case, punctuation and accents dropped, so "Bëbbi," and "bebbi" compare equal. */
private fun plain(text: String): String =
    Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .filter { it.isLetter() || it == '-' }
        .trim('-')
