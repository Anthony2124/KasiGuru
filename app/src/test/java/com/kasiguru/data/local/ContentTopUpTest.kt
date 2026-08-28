package com.kasiguru.data.local

import com.kasiguru.data.local.entity.VocabularyEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the upgrade path for an install that already exists.
 *
 * A migration adds columns, not rows. Without this top-up a learner upgrading into the release that
 * tripled the corpus would have kept their old word list and three empty definition columns. The
 * risk in fixing that is the opposite failure -- touching a learning column while topping content
 * up -- so that is what most of these assertions are about.
 */
class ContentTopUpTest {

    private val learned = VocabularyEntity(
        id = 7,
        kasiguranin = "singët",
        tagalog = "langgam",
        english = "ant",
        category = "Animals & Wildlife",
        isLearned = true,
        timesReviewed = 14,
        easinessFactor = 1.6,
        intervalDays = 4,
        nextReviewDate = "2026-08-22",
        lapses = 6,
        relearningStep = 2
    )

    private val corpusAnt = VocabularyEntity(
        kasiguranin = "singët",
        tagalog = "langgam",
        english = "ant",
        category = "Animals & Wildlife",
        meaningEnglish = "A small insect that lives in large colonies and marches in lines.",
        meaningTagalog = "Maliit na insektong namumuhay nang pangkat."
    )

    @Test
    fun aWordTheDeviceDoesNotHaveIsInserted() {
        val newWord = VocabularyEntity(kasiguranin = "lutá", english = "land, earth, soil")

        val plan = ContentTopUp.plan(listOf(learned), listOf(corpusAnt, newWord))

        assertEquals(1, plan.toInsert.size)
        assertEquals("lutá", plan.toInsert.single().kasiguranin)
        assertEquals("Room must assign the id", 0, plan.toInsert.single().id)
    }

    @Test
    fun aMissingDefinitionIsFilledInWithoutDisturbingWhatTheLearnerHasDone() {
        val plan = ContentTopUp.plan(listOf(learned), listOf(corpusAnt))

        assertTrue(plan.toInsert.isEmpty())
        val patched = plan.toUpdate.single()
        assertEquals("the row must be updated, not duplicated", 7, patched.id)
        assertEquals(corpusAnt.meaningEnglish, patched.meaningEnglish)
        assertEquals(corpusAnt.meaningTagalog, patched.meaningTagalog)

        // The whole point of the guard.
        assertTrue(patched.isLearned)
        assertEquals(14, patched.timesReviewed)
        assertEquals(1.6, patched.easinessFactor, 0.0001)
        assertEquals(4, patched.intervalDays)
        assertEquals("2026-08-22", patched.nextReviewDate)
        assertEquals(6, patched.lapses)
        assertEquals(2, patched.relearningStep)
    }

    @Test
    fun aDefinitionAlreadyOnTheDeviceIsLeftAlone() {
        val edited = learned.copy(meaningEnglish = "a definition a moderator wrote")

        val plan = ContentTopUp.plan(listOf(edited), listOf(corpusAnt))

        // Only the Tagalog side was blank, so that is all that changes.
        val patched = plan.toUpdate.single()
        assertEquals("a definition a moderator wrote", patched.meaningEnglish)
        assertEquals(corpusAnt.meaningTagalog, patched.meaningTagalog)
    }

    /** Homonyms are separate senses, and topping up must not fold them together. */
    @Test
    fun eachSenseOfAHomonymIsTreatedSeparately() {
        val lungs = VocabularyEntity(kasiguranin = "baga", english = "lungs", meaningEnglish = "The organs used for breathing.")
        val ember = VocabularyEntity(kasiguranin = "baga", english = "coal, ember", meaningEnglish = "Wood still glowing in a fire.")
        val deviceHasLungsOnly = listOf(VocabularyEntity(id = 3, kasiguranin = "baga", english = "lungs"))

        val plan = ContentTopUp.plan(deviceHasLungsOnly, listOf(lungs, ember))

        assertEquals("the ember sense is missing and must be added", 1, plan.toInsert.size)
        assertEquals("coal, ember", plan.toInsert.single().english)
        assertEquals("the lungs sense only needs its definition", 1, plan.toUpdate.size)
        assertEquals(3, plan.toUpdate.single().id)
    }

    @Test
    fun aFreshDatabaseIsLeftToTheSeedingCallback() {
        val plan = ContentTopUp.plan(emptyList(), listOf(corpusAnt))

        assertTrue(plan.isEmpty)
    }

    @Test
    fun aSecondRunFindsNothingToDo() {
        val upToDate = learned.copy(
            meaningEnglish = corpusAnt.meaningEnglish,
            meaningTagalog = corpusAnt.meaningTagalog
        )

        val plan = ContentTopUp.plan(listOf(upToDate), listOf(corpusAnt))

        assertTrue(plan.isEmpty)
    }

    /** The shipped corpus must be internally consistent: every sense unique, every one defined. */
    @Test
    fun theShippedCorpusHasOneDefinitionPerDistinctSense() {
        val corpus = DatabaseSeeder.getInitialVocabulary()

        val undefined = corpus.filter { it.meaningEnglish.isBlank() || it.meaningTagalog.isBlank() }
        assertTrue("every entry needs both definitions, missing on: " +
            undefined.take(5).joinToString { it.kasiguranin + "/" + it.english }, undefined.isEmpty())

        val keys = corpus.map { it.kasiguranin.lowercase() + "|" + it.english.lowercase() }
        assertEquals("no sense may appear twice", keys.size, keys.toSet().size)
    }
}
