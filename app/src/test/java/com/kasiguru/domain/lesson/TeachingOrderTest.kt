package com.kasiguru.domain.lesson

import com.kasiguru.data.local.entity.VocabularyEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the order a section teaches its words in.
 *
 * This exists because of a real complaint: the first lesson of a section opened on *dakëp* — "catch,
 * apprehend" — in a section titled "greetings". Two separate faults produced that, and this covers
 * the second one: lessons sliced their section alphabetically, so lesson one of every section was
 * whatever happened to start with "a".
 */
class TeachingOrderTest {

    private fun word(headword: String, pos: String) =
        VocabularyEntity(kasiguranin = headword, english = "gloss of $headword", partOfSpeech = pos)

    @Test
    fun nounsAreTaughtBeforeVerbsAndDescribingWords() {
        val words = listOf(
            word("zamora", "Noun"),
            word("abante", "Verb"),
            word("bigsək", "Adjective"),
            word("aldew", "Noun")
        )

        val order = LearningTree.teachingOrder(words).map { it.kasiguranin }

        // Both nouns first, in spite of "abante" and "bigsək" sorting earlier alphabetically.
        assertEquals(listOf("aldew", "zamora", "abante", "bigsək"), order)
    }

    @Test
    fun orderIsStableSoALessonTeachesTheSameSevenWordsEveryTime() {
        // LessonPlan slices by position and progress is recorded per index, so a reshuffle between
        // runs would detach a learner's completed lesson from the words they actually studied.
        val words = (1..20).map { word("w%02d".format(it), if (it % 3 == 0) "Verb" else "Noun") }

        val first = LearningTree.teachingOrder(words.shuffled()).map { it.kasiguranin }
        val second = LearningTree.teachingOrder(words.shuffled()).map { it.kasiguranin }

        assertEquals(first, second)
    }

    @Test
    fun anUnrecognisedPartOfSpeechSortsWithTheTailRatherThanJumpingTheQueue() {
        val words = listOf(
            word("aaa", ""),
            word("bbb", "Noun"),
            word("ccc", "Interjection")
        )

        val order = LearningTree.teachingOrder(words).map { it.kasiguranin }

        assertEquals("bbb", order.first())
        assertEquals(setOf("aaa", "ccc"), order.drop(1).toSet())
    }
}
