package com.kasiguru.data.remote

import com.kasiguru.data.local.entity.VocabularyEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the line between what the admin portal owns and what only this device knows.
 *
 * This is the failure the test exists for, seen on a real device: a word seeded with six lapses came
 * back from a content sync with zero, because the merge rebuilt the row from the cloud and copied a
 * hand-written list of learning fields back. Anything not on that list was quietly destroyed.
 */
class VocabularyContentMergeTest {

    private val local = VocabularyEntity(
        id = 42,
        kasiguranin = "singët",
        tagalog = "old spelling",
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

    private val cloud = VocabularyEntity(
        kasiguranin = "singët",
        tagalog = "langgam",
        english = "ant",
        category = "Animals & Wildlife",
        exampleSentence = "Adu ay singët.",
        exampleTranslation = "There are many ants.",
        exampleSentence2 = "Nakakagat ang singët.",
        exampleTranslation2 = "The ant bit me.",
        ipaNotation = "ˈsi.ŋət"
    )

    @Test
    fun everyLearningFieldSurvivesAContentSync() {
        val merged = VocabularyContentMerge.merge(local, cloud)

        assertEquals(42, merged.id)
        assertTrue(merged.isLearned)
        assertEquals(14, merged.timesReviewed)
        assertEquals(1.6, merged.easinessFactor, 0.0001)
        assertEquals(4, merged.intervalDays)
        assertEquals("2026-08-22", merged.nextReviewDate)
        assertEquals("the lapse history is not the admin's to reset", 6, merged.lapses)
        assertEquals(2, merged.relearningStep)
    }

    @Test
    fun editorialContentComesFromTheCloud() {
        val merged = VocabularyContentMerge.merge(local, cloud)

        assertEquals("langgam", merged.tagalog)
        assertEquals("Adu ay singët.", merged.exampleSentence)
        assertEquals("ˈsi.ŋət", merged.ipaNotation)
    }

    @Test
    fun theSecondExampleReachesTheLearner() {
        // Written by the admin word form since v25, parsed by nobody until now.
        val merged = VocabularyContentMerge.merge(local, cloud)

        assertEquals("Nakakagat ang singët.", merged.exampleSentence2)
        assertEquals("The ant bit me.", merged.exampleTranslation2)
    }

    @Test
    fun aPartialSourceNeverBlanksWhatItDoesNotCarry() {
        // The realtime listener reads six fields under legacy names. Treating its silence about the
        // aspect forms as "clear them" wiped real content on every launch.
        val rich = local.copy(
            neutralForm = "sumingët",
            perfectiveForm = "nasingët",
            exampleSentence2 = "Nakita ko ang singët.",
            ipaNotation = "ˈsi.ŋət"
        )
        val partial = VocabularyEntity(kasiguranin = "singët", tagalog = "langgam", english = "ant")

        val merged = VocabularyContentMerge.mergeNonBlank(rich, partial)

        assertEquals("langgam", merged.tagalog)
        assertEquals("sumingët", merged.neutralForm)
        assertEquals("nasingët", merged.perfectiveForm)
        assertEquals("Nakita ko ang singët.", merged.exampleSentence2)
        assertEquals("ˈsi.ŋət", merged.ipaNotation)
        assertEquals(6, merged.lapses)
        assertEquals(2, merged.relearningStep)
        assertEquals(14, merged.timesReviewed)
    }

    /**
     * The admin word form has no input for the root form, the audio file name or the two phonetic
     * flags, so a document it writes simply omits those keys and the parser turns them into "" and
     * false. Copying that over the local row meant editing any word in the portal silently erased
     * its seeded root form and its audio reference on the next full reconcile.
     */
    @Test
    fun anAdminEditDoesNotEraseFieldsTheAdminFormCannotWrite() {
        val seeded = local.copy(
            rootForm = "singët",
            audioFileName = "singet_audio",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            meaningEnglish = "A small insect that lives in large colonies.",
            meaningTagalog = "Maliit na insektong namumuhay nang pangkat."
        )
        // Exactly what the portal saves: the fields it has controls for, and nothing else.
        val adminEdit = VocabularyEntity(
            kasiguranin = "singët",
            tagalog = "langgam",
            english = "ant",
            category = "Animals & Wildlife"
        )

        val merged = VocabularyContentMerge.merge(seeded, adminEdit)

        assertEquals("langgam", merged.tagalog)
        assertEquals("singët", merged.rootForm)
        assertEquals("singet_audio", merged.audioFileName)
        assertTrue(merged.phoneticGlottal)
        assertTrue(merged.phoneticVowelLength)
        assertEquals("A small insect that lives in large colonies.", merged.meaningEnglish)
        assertEquals("Maliit na insektong namumuhay nang pangkat.", merged.meaningTagalog)
        assertEquals(6, merged.lapses)
    }

    /** A definition written in the portal reaches the device. */
    @Test
    fun aDefinitionWrittenInThePortalOverwritesTheSeededOne() {
        val seeded = local.copy(meaningEnglish = "seeded definition")
        val edited = cloud.copy(meaningEnglish = "definition written by a moderator")

        val merged = VocabularyContentMerge.merge(seeded, edited)

        assertEquals("definition written by a moderator", merged.meaningEnglish)
    }

    @Test
    fun aWordThisDeviceHasNeverSeenIsInsertedFresh() {
        val merged = VocabularyContentMerge.merge(null, cloud)

        assertEquals("a new row must let Room assign the id", 0, merged.id)
        assertEquals("langgam", merged.tagalog)
        assertEquals(0, merged.lapses)
    }
}
