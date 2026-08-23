package com.kasiguru.domain.lesson

import com.kasiguru.data.local.entity.VocabularyEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the prompt against printing its own answer.
 *
 * Found on a device: "Say this in Kasiguranin — buhay" appeared above an option list that contained
 * `buhay`, because the Tagalog gloss of a shared word is the headword itself. Roughly a tenth of the
 * corpus glosses to itself that way, so this is not a rare edge.
 */
class ExercisePromptTest {

    private fun exercise(word: VocabularyEntity, promptIsKasiguranin: Boolean) =
        Exercise.ChooseTranslation(
            word = word,
            options = listOf(word.kasiguranin),
            answer = word.kasiguranin,
            promptIsKasiguranin = promptIsKasiguranin
        )

    @Test
    fun theMeaningDirectionNeverPrintsTheHeadword() {
        val buhay = VocabularyEntity(kasiguranin = "buhay", tagalog = "buhay", english = "alive")
        assertEquals("alive", exercise(buhay, promptIsKasiguranin = false).prompt)
    }

    @Test
    fun anOrdinaryWordStillPromptsInTagalog() {
        val apak = VocabularyEntity(kasiguranin = "apak", tagalog = "daras", english = "adze")
        assertEquals("daras", exercise(apak, promptIsKasiguranin = false).prompt)
    }

    @Test
    fun theKasiguraninDirectionPromptsWithTheHeadword() {
        val apak = VocabularyEntity(kasiguranin = "apak", tagalog = "daras", english = "adze")
        assertEquals("apak", exercise(apak, promptIsKasiguranin = true).prompt)
    }

    @Test
    fun aWordWithNoUsableGlossFallsBackRatherThanShowingNothing() {
        // ExerciseGenerator flips these to the other direction, so this is a totality guarantee for
        // the getter rather than a shape the learner should ever meet.
        val kuwan = VocabularyEntity(kasiguranin = "kuwan", tagalog = "kuwan", english = "")
        assertEquals("kuwan", exercise(kuwan, promptIsKasiguranin = false).prompt)
    }
}
