package com.kasiguru.domain.lesson

import com.kasiguru.data.local.entity.VocabularyEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards what gets woven into a lesson.
 *
 * The rule that matters is the one that is easy to get wrong: a "revisited" word from the lesson's
 * own category is not interleaving at all, because the learner is still inside the same field and
 * can still answer by elimination.
 */
class InterleavingTest {

    private fun word(id: Int, category: String) =
        VocabularyEntity(id = id, kasiguranin = "w$id", english = "e$id", category = category)

    private val lesson = (1..7).map { word(it, "Tools") }

    @Test
    fun leechesAreWovenInAheadOfMerelyDueWords() {
        val leech = word(20, "Greetings")
        val due = word(30, "Food")

        val composed = Interleaving.compose(lesson, listOf(leech), listOf(due), limit = 1)

        assertEquals(8, composed.size)
        assertEquals(20, composed.last().id)
    }

    @Test
    fun revisitedWordsAreAppendedAfterTheNewMaterial() {
        val composed = Interleaving.compose(
            lesson,
            leeches = emptyList(),
            due = listOf(word(30, "Food"), word(31, "Food"))
        )

        assertEquals(lesson.map { it.id }, composed.take(7).map { it.id })
        assertEquals(listOf(30, 31), composed.drop(7).map { it.id })
    }

    @Test
    fun aWordFromTheLessonOwnCategoryIsNotInterleaving() {
        val sameField = word(21, "Tools")
        val composed = Interleaving.compose(lesson, listOf(sameField), emptyList())

        assertEquals(lesson.map { it.id }, composed.map { it.id })
    }

    @Test
    fun aWordAlreadyInTheLessonIsNeverAddedTwice() {
        val composed = Interleaving.compose(lesson, emptyList(), listOf(lesson.first()))
        assertEquals(7, composed.size)
    }

    @Test
    fun neverMoreThanTheLimit() {
        val due = (30..40).map { word(it, "Food") }
        val composed = Interleaving.compose(lesson, emptyList(), due)

        assertEquals(7 + Interleaving.REVISITED_PER_LESSON, composed.size)
    }

    @Test
    fun anEmptyLessonStaysEmpty() {
        // A category can be too small to fill a lesson; revisited words must not conjure one.
        assertTrue(Interleaving.compose(emptyList(), listOf(word(1, "Food")), emptyList()).isEmpty())
    }

    @Test
    fun nothingToRevisitLeavesTheLessonUnchanged() {
        assertEquals(
            lesson.map { it.id },
            Interleaving.compose(lesson, emptyList(), emptyList()).map { it.id }
        )
    }
}
