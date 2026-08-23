package com.kasiguru.domain.lesson

import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.util.AnswerLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Guards the line shown after a wrong answer.
 *
 * The rule worth pinning is when to say nothing: a contrast that restates the same word, or that
 * names a word the learner did not choose, is worse than the plain verdict it replaces.
 */
class RemediationTest {

    private val apak = VocabularyEntity(
        id = 1, kasiguranin = "apak", tagalog = "daras", english = "adze", category = "Tools"
    )
    private val palakol = VocabularyEntity(
        id = 2, kasiguranin = "palakul", tagalog = "palakol", english = "axe", category = "Tools"
    )

    @Test
    fun choosingAnotherHeadwordSaysWhatThatWordMeans() {
        assertEquals(
            "You chose palakul, which means palakol · axe.",
            Remediation.contrastLine("palakul", palakol, apak)
        )
    }

    @Test
    fun choosingAnotherGlossNamesTheWordItBelongsTo() {
        assertEquals(
            "You chose palakol · axe, which is palakul.",
            Remediation.contrastLine("palakol · axe", palakol, apak)
        )
    }

    @Test
    fun thereIsNothingToSayAboutTheWordItself() {
        // The aspect exercises build every option from one entry, so the chosen form and the answer
        // are the same word. "You chose apak, which means adze" beside the answer apak is noise.
        assertNull(Remediation.contrastLine("apakan", apak, apak))
    }

    @Test
    fun aTypedNonWordProducesNoLine() {
        assertNull(Remediation.contrastLine("zzzz", null, apak))
    }

    @Test
    fun aGlossThatRepeatsTheHeadwordIsNotPrintedTwice() {
        val kuwan = VocabularyEntity(
            id = 3, kasiguranin = "kuwan", tagalog = "kuwan", english = "thingamajig"
        )
        assertEquals(
            "You chose kuwan, which means thingamajig.",
            Remediation.contrastLine("kuwan", kuwan, apak)
        )
    }

    @Test
    fun anOptionLabelIsSearchableWholeAndInParts() {
        assertEquals(
            listOf("palakol · axe", "palakol", "axe"),
            AnswerLabel.candidates("palakol · axe")
        )
        assertEquals(listOf("apak"), AnswerLabel.candidates("  apak  "))
        assertEquals(emptyList<String>(), AnswerLabel.candidates("   "))
    }
}
