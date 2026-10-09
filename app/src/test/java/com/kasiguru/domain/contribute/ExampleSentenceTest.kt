package com.kasiguru.domain.contribute

import com.kasiguru.data.local.entity.VocabularyEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What a learner's example sentence must have before it is worth a verifier's time. */
class ExampleSentenceTest {

    private val word = VocabularyEntity(kasiguranin = "bëbbi", english = "woman")
    private val verb = VocabularyEntity(kasiguranin = "kain", english = "eat", perfectiveForm = "kinain")

    @Test
    fun `a sentence using the word with a translation is accepted`() {
        assertNull(exampleSentenceProblem(word, "Mapiya i bëbbi ya.", "The woman is kind."))
    }

    @Test
    fun `the word may be typed without its accent`() {
        assertNull(exampleSentenceProblem(word, "Mapiya i bebbi ya", "The woman is kind."))
    }

    @Test
    fun `a recorded verb form or an affixed form counts as the word`() {
        assertNull(exampleSentenceProblem(verb, "Kinain ko i saging", "I ate the banana."))
        assertNull(exampleSentenceProblem(verb, "Kumakain kami ngayon", "We are eating now."))
    }

    @Test
    fun `a sentence without the word is refused`() {
        assertEquals("Use \"bëbbi\" (or one of its forms) in the sentence.",
            exampleSentenceProblem(word, "Mapiya i lalëkke ya", "The man is kind."))
    }

    @Test
    fun `a sentence under three words is refused`() {
        assertEquals("Write a sentence of at least 3 words.", exampleSentenceProblem(word, "bëbbi ya", "That woman."))
    }

    @Test
    fun `the English translation is required`() {
        assertEquals("Add what the sentence means in English.", exampleSentenceProblem(word, "Mapiya i bëbbi ya", " "))
    }
}
