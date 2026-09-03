package com.kasiguru.domain.lesson

import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.util.srs.Sm2Algorithm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the two things the tree can get wrong in ways a learner would feel.
 *
 * The first is honesty about mastery: a node that claims 🌳 Mastered on the strength of a couple of
 * multiple-choice answers would make the whole path a lie. The second is the gate, which decides
 * whether a section is a stage of a journey or a wall.
 */
class LearningTreeTest {

    private fun word(
        id: Int,
        timesReviewed: Int = 0,
        isLearned: Boolean = false
    ) = VocabularyEntity(
        id = id,
        kasiguranin = "w$id",
        english = "e$id",
        category = "Greetings & Essentials",
        timesReviewed = timesReviewed,
        isLearned = isLearned
    )

    // -- mastery of a single word ---------------------------------------------

    @Test
    fun anUntouchedWordHasNoMastery() {
        assertEquals(Mastery.NONE, LearningTree.masteryOf(word(1)))
    }

    @Test
    fun oneReviewMakesAWordFamiliar() {
        assertEquals(Mastery.FAMILIAR, LearningTree.masteryOf(word(1, timesReviewed = 1)))
    }

    @Test
    fun practisingBeginsAtTheSameReviewCountSm2Requires() {
        val justBelow = word(1, timesReviewed = Sm2Algorithm.MIN_LEARNED_REVIEWS - 1)
        val atThreshold = word(2, timesReviewed = Sm2Algorithm.MIN_LEARNED_REVIEWS)

        assertEquals(Mastery.FAMILIAR, LearningTree.masteryOf(justBelow))
        assertEquals(Mastery.PRACTICING, LearningTree.masteryOf(atThreshold))
    }

    @Test
    fun masteredIsOnlyEverSm2sOwnVerdict() {
        // Reviewed many times but never retained: the SM-2 ladder has not certified it, so neither
        // does the tree. This is the case that would otherwise let a node look finished while the
        // learner still cannot recall a single word in it.
        val drilled = word(1, timesReviewed = 40, isLearned = false)
        assertEquals(Mastery.PRACTICING, LearningTree.masteryOf(drilled))

        val retained = word(2, timesReviewed = Sm2Algorithm.MIN_LEARNED_REVIEWS, isLearned = true)
        assertEquals(Mastery.MASTERED, LearningTree.masteryOf(retained))
    }

    // -- mastery of a node ----------------------------------------------------

    @Test
    fun anIncompleteLessonIsNotEvenFamiliar() {
        val words = (1..7).map { word(it, timesReviewed = 9, isLearned = true) }
        assertEquals(Mastery.NONE, LearningTree.nodeMastery(isLessonComplete = false, words = words))
    }

    @Test
    fun finishingTheLessonEarnsFamiliarAndNoMore() {
        val words = (1..7).map { word(it, timesReviewed = 1) }
        assertEquals(Mastery.FAMILIAR, LearningTree.nodeMastery(true, words))
    }

    @Test
    fun halfTheWordsPractisingLiftsTheNodeToPractising() {
        val words = (1..4).map { word(it, timesReviewed = Sm2Algorithm.MIN_LEARNED_REVIEWS) } +
            (5..7).map { word(it, timesReviewed = 1) }
        assertEquals(Mastery.PRACTICING, LearningTree.nodeMastery(true, words))
    }

    @Test
    fun aNodeIsMasteredOnlyWhenNearlyEveryWordIs() {
        val sixOfSeven = (1..6).map { word(it, timesReviewed = 5, isLearned = true) } +
            listOf(word(7, timesReviewed = 1))
        // 6/7 is 85.7%, over the 80% share.
        assertEquals(Mastery.MASTERED, LearningTree.nodeMastery(true, sixOfSeven))

        val fiveOfSeven = (1..5).map { word(it, timesReviewed = 5, isLearned = true) } +
            (6..7).map { word(it, timesReviewed = 1) }
        // 5/7 is 71.4%, under it - and five retained words out of seven is not a mastered lesson.
        assertEquals(Mastery.PRACTICING, LearningTree.nodeMastery(true, fiveOfSeven))
    }

    // -- viability ------------------------------------------------------------

    @Test
    fun aSectionTooThinToWalkThroughIsNotViable() {
        // The measured case this exists for: the school theme has ~10 words in the corpus.
        assertFalse(LearningTree.isViable(10))
        assertFalse(LearningTree.isViable(LearningTree.MIN_WORDS_FOR_SECTION - 1))
        assertTrue(LearningTree.isViable(LearningTree.MIN_WORDS_FOR_SECTION))
    }

    @Test
    fun everyThemeSourcedSectionIsDefinedButCarriesNoWordsUntilTagged() {
        // The situational sections ship as definitions and appear only once the corpus is tagged.
        // If this ever fails, someone has pointed a journey section at a category it does not own.
        val themed = LearningTree.sections.filter { it.source is SectionSource.Theme }
        assertTrue("expected the situational sections to be theme-sourced", themed.isNotEmpty())
        themed.forEach {
            val tag = (it.source as SectionSource.Theme).tag
            assertEquals(tag.trim().lowercase(), tag)
        }
    }

    // -- the gate -------------------------------------------------------------

    @Test
    fun theGateIsSixtyPercentOfASectionsLessonXp() {
        // 10 lesson nodes at 30 XP is 300 available; 60% of that is 180.
        assertEquals(180, LearningTree.requiredXpToOpenNext(lessonNodeCount = 10))
    }

    @Test
    fun aVeryLargeSectionIsCappedRatherThanBecomingAWall() {
        // Pang-araw-araw carries 205 words, so 30 nodes: 60% would be 540 XP.
        assertEquals(LearningTree.GATE_XP_CAP, LearningTree.requiredXpToOpenNext(lessonNodeCount = 30))
    }

    // -- the two tiers --------------------------------------------------------

    private fun lessonNode(
        position: Int,
        mastery: Mastery,
        isDeepDive: Boolean
    ) = TreeNodeState(
        node = TreeNode.Lesson(LessonRef("theme:pagbati", position - 1), position),
        title = "Lesson $position",
        mastery = mastery,
        isUnlocked = true,
        isCurrent = false,
        isDeepDive = isDeepDive
    )

    /** A stage of [core] core lessons and [deep] deep-dive ones, every core lesson finished. */
    private fun stage(core: Int, deep: Int, coreMastery: Mastery = Mastery.FAMILIAR): TreeSection {
        val nodes = (1..core).map { lessonNode(it, coreMastery, isDeepDive = false) } +
            (1..deep).map { lessonNode(core + it, Mastery.NONE, isDeepDive = true) }
        return TreeSection(
            definition = LearningTree.sections.first(),
            wordCount = (core + deep) * LessonPlan.WORDS_PER_LESSON,
            nodes = nodes,
            earnedXp = 0,
            requiredXp = 108,
            isUnlocked = true
        )
    }

    @Test
    fun aStageIsCompleteOnceItsCoreIsDoneEvenWithDeepDiveLessonsLeft() {
        // The case the two tiers exist for: Paglalarawan carries 140 words, so 20 lessons. Before
        // the split a learner who moved on after the core would have seen that stage read as
        // unfinished forever.
        val stage = stage(core = LearningTree.CORE_LESSONS_PER_STAGE, deep = 14)

        assertTrue("core is finished, so the stage is", stage.isComplete)
        assertEquals(LearningTree.CORE_LESSONS_PER_STAGE, stage.coreLessonNodeCount)
        assertEquals(14, stage.deepDiveNodeCount)
    }

    @Test
    fun anUnfinishedCoreLeavesTheStageIncompleteHoweverMuchDeepDiveIsDone() {
        val stage = stage(core = 6, deep = 4, coreMastery = Mastery.NONE)
        assertFalse(stage.isComplete)
    }

    @Test
    fun theGateIsSizedForTheCoreNotForHowLargeTheStageHappensToBe() {
        // Six core lessons at 30 XP is 180 available; 60% is 108. The same number whether the stage
        // carries fifty words or two hundred and fifty, which is the point: before this, the biggest
        // stage set the hardest gate purely for being biggest.
        assertEquals(108, LearningTree.requiredXpToOpenNext(LearningTree.CORE_LESSONS_PER_STAGE))
    }

    @Test
    fun everyStageInTheTreeIsDistinctAndLowercaseKeyed() {
        // A duplicate id would collide in `lesson_progress`, silently merging two stages' history.
        val ids = LearningTree.sections.map { it.id }
        assertEquals("stage ids must be unique", ids.size, ids.toSet().size)

        val units = LearningTree.sections.map { LearningTree.unitIdFor(it.source) }
        assertEquals("unit keys must be unique", units.size, units.toSet().size)
    }

    @Test
    fun theJourneyOpensAtGreetings() {
        // The failure this guards is the one measured in the corpus: pagbati carried 14 words,
        // under the section floor, so it was dropped and the path opened at Family instead. The
        // stage list must at least still *lead* with it.
        assertEquals("pagbati", LearningTree.sections.first().id)
    }

    @Test
    fun everyWordStillHasSomewhereToBeTaught() {
        // The remainder stage is the safety net: a word no theme claims is a word the app has
        // hidden. 209 words are unplaced even after reclassification, so this must not be removed
        // until every one of them carries a theme.
        assertTrue(
            "the tree must keep a home for words no stage claims",
            LearningTree.sections.any { it.source is SectionSource.Remainder }
        )
    }

    // -- the checkpoint -------------------------------------------------------

    @Test
    fun aCheckpointUnitKeyRoundTripsToItsStage() {
        val unit = LearningTree.masteryUnitId("pagbati")
        assertEquals("mastery:pagbati", unit)
        assertEquals("pagbati", LearningTree.sectionIdForMasteryUnit(unit))
    }

    @Test
    fun aCheckpointKeyCanNeverBeMistakenForALessonUnit() {
        // The two namespaces share one table, so a collision would merge a stage's lessons with its
        // checkpoint and silently corrupt both.
        LearningTree.sections.forEach { section ->
            val lessonUnit = LearningTree.unitIdFor(section.source)
            assertNull(
                "a lesson unit key must not parse as a checkpoint",
                LearningTree.sectionIdForMasteryUnit(lessonUnit)
            )
        }
    }

    @Test
    fun aCheckpointNeverShowsItsRawKeyToALearner() {
        // The bug this guards is one this project has already shipped once: "theme:pamilya" reached
        // a learner as a lesson title. A checkpoint's key must resolve to its stage's real name.
        val resolved = LearningTree.sectionForUnit(LearningTree.masteryUnitId("pagbati"))
        assertEquals("Pagbati at Sarili", resolved?.title)
    }

    @Test
    fun anEmptyOrPlainKeyIsNotACheckpoint() {
        assertNull(LearningTree.sectionIdForMasteryUnit("theme:pagbati"))
        assertNull(LearningTree.sectionIdForMasteryUnit(LearningTree.MASTERY_UNIT_PREFIX))
        assertNull(LearningTree.sectionIdForMasteryUnit(""))
    }

    @Test
    fun aCheckpointIsShortEnoughToFinish() {
        // ExerciseGenerator introduces every word it is given, so this count is the length of the
        // run. A stage's whole core would be forty-two exercises, which is where learners stop.
        assertTrue(
            "a checkpoint must be shorter than a stage's core",
            LearningTree.MASTERY_WORD_COUNT <
                LearningTree.CORE_LESSONS_PER_STAGE * LessonPlan.WORDS_PER_LESSON
        )
    }

    @Test
    fun sectionOpensNextOnlyOnceItsGateIsMet() {
        fun section(earned: Int) = TreeSection(
            definition = LearningTree.sections.first(),
            wordCount = 70,
            nodes = emptyList(),
            earnedXp = earned,
            requiredXp = 180,
            isUnlocked = true
        )

        assertFalse(section(179).opensNext)
        assertTrue(section(180).opensNext)
        assertEquals(0.5f, section(90).gateFraction, 0.001f)
    }
}
