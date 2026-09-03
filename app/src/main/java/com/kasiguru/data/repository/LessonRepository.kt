package com.kasiguru.data.repository

import com.kasiguru.data.local.dao.LessonDao
import com.kasiguru.data.local.entity.LessonProgressEntity
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.domain.lesson.Exercise
import com.kasiguru.domain.lesson.ExerciseGenerator
import com.kasiguru.domain.lesson.Interleaving
import com.kasiguru.domain.lesson.LearningTree
import com.kasiguru.domain.lesson.LessonPlan
import com.kasiguru.domain.lesson.LessonRef
import com.kasiguru.domain.lesson.LessonUnit
import com.kasiguru.domain.lesson.Mastery
import com.kasiguru.domain.lesson.SectionDefinition
import com.kasiguru.domain.lesson.SectionSource
import com.kasiguru.domain.lesson.TreeNode
import com.kasiguru.domain.lesson.TreeNodeState
import com.kasiguru.domain.lesson.TreeSection
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The lesson system.
 *
 * Units and lessons are *derived* from the vocabulary corpus rather than authored, so this repository
 * owns the join between the dictionary (which the admin portal edits) and the learner's progress
 * (which only the app writes). See [LessonPlan] for why the split works that way.
 */
@Singleton
class LessonRepository @Inject constructor(
    private val lessonDao: LessonDao,
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository,
    private val exerciseGenerator: ExerciseGenerator
) {

    fun observeProgress(): Flow<List<LessonProgressEntity>> = lessonDao.observeAll()

    fun observeCompletedCount(): Flow<Int> = lessonDao.observeCompletedCount()

    /** Every unit with its completion state, in the order the tree walks them. */
    suspend fun units(): List<LessonUnit> {
        val wordsByUnit = allWordsByUnit()
        val completed = lessonDao.getAllOnce()
            .filter { it.isComplete }
            .groupingBy { it.unitId }
            .eachCount()

        return wordsByUnit.map { (unitId, words) ->
            LessonUnit(
                id = unitId,
                title = unitId,
                wordCount = words.size,
                lessonCount = LessonPlan.lessonCountFor(words.size),
                completedLessons = completed[unitId] ?: 0
            )
        }
    }

    /**
     * The tree: every viable section, with the learner's state applied.
     *
     * Sections are assembled, never stored. A section's lesson nodes are its theme's [LessonPlan]
     * slices, so a node's identity is the same `(unitId, lessonIndex)` the lesson player and
     * [LessonProgressEntity] already use - the tree is a route through existing lessons, not a
     * parallel set of them.
     *
     * Sections below [LearningTree.MIN_WORDS_FOR_SECTION] are dropped rather than shown as a stub the
     * learner cannot finish. Their words are not lost with them: [allWordsByUnit] hands anything a
     * shipping section does not claim to the closing remainder section.
     */
    suspend fun treeSections(): List<TreeSection> {
        val wordsByUnit = allWordsByUnit()
        val progress = lessonDao.getAllOnce().associateBy { it.unitId to it.lessonIndex }

        var previousOpensNext = true
        return LearningTree.sections.mapNotNull { definition ->
            val units = unitIdsFor(definition)
            val words = units.flatMap { wordsByUnit[it].orEmpty() }
            if (!LearningTree.isViable(words.size)) return@mapNotNull null

            val isUnlocked = previousOpensNext
            val nodes = buildNodes(definition, units, wordsByUnit, progress, isUnlocked)
            val earnedXp = units.sumOf { unitId ->
                progress.values
                    .filter { it.unitId == unitId && it.isComplete }
                    .sumOf { LessonPlan.xpFor(it.bestAccuracy) }
            }

            val section = TreeSection(
                definition = definition,
                wordCount = words.size,
                nodes = nodes,
                earnedXp = earnedXp,
                // Measured against the core only. Gating on every lesson made a large stage a wall
                // purely for being large -- the reason GATE_XP_CAP had to exist -- and would now
                // also demand the optional tail the deep-dive tier exists to make optional.
                requiredXp = LearningTree.requiredXpToOpenNext(
                    nodes.count { it.node is TreeNode.Lesson && !it.isDeepDive }
                ),
                isUnlocked = isUnlocked
            )
            // A locked section cannot open the one after it, or a single unreachable section would
            // cascade the whole rest of the tree open.
            previousOpensNext = isUnlocked && section.opensNext
            section
        }
    }

    /**
     * One section's nodes: a lesson per [LessonPlan] slice, then the section's mastery test.
     *
     * `isCurrent` marks the first unfinished lesson, which is the single node the path should draw
     * the eye to. The mastery test unlocks only once every lesson in the section is complete - it
     * tests the section, so it has nothing to test until the section has been taught.
     */
    private fun buildNodes(
        definition: SectionDefinition,
        units: List<String>,
        wordsByUnit: Map<String, List<VocabularyEntity>>,
        progress: Map<Pair<String, Int>, LessonProgressEntity>,
        isSectionUnlocked: Boolean
    ): List<TreeNodeState> {
        val core = mutableListOf<TreeNodeState>()
        val deepDive = mutableListOf<TreeNodeState>()
        var currentMarked = false
        var position = 0

        units.forEach { unitId ->
            val unitWords = wordsByUnit[unitId].orEmpty()
            for (index in 0 until LessonPlan.lessonCountFor(unitWords.size)) {
                val ref = LessonRef(unitId, index)
                val isComplete = progress[unitId to index]?.isComplete == true
                val range = LessonPlan.wordIndicesFor(index, unitWords.size)
                val lessonWords = if (range.isEmpty()) emptyList() else unitWords.slice(range)

                position++
                // Everything past the core tier is a deep dive. The lesson itself is unchanged --
                // same slice, same `(unitId, lessonIndex)`, same progress row -- so a learner who
                // already finished lesson 12 of a large stage keeps it; it simply now sits after the
                // checkpoint instead of before it.
                val isDeepDive = position > LearningTree.CORE_LESSONS_PER_STAGE

                // Only a core lesson can be the one node the path points at. Pointing the learner
                // into the optional tail is precisely what the two tiers exist to stop.
                val isCurrent = isSectionUnlocked && !isComplete && !currentMarked && !isDeepDive
                if (isCurrent) currentMarked = true

                val state = TreeNodeState(
                    node = TreeNode.Lesson(ref, position),
                    title = "Lesson $position",
                    mastery = LearningTree.nodeMastery(isComplete, lessonWords),
                    isUnlocked = isSectionUnlocked,
                    isCurrent = isCurrent,
                    isDeepDive = isDeepDive
                )
                if (isDeepDive) deepDive += state else core += state
            }
        }

        // The checkpoint tests the core, so it opens when the core is done rather than waiting on an
        // optional tail the learner may never walk.
        val coreDone = core.isNotEmpty() && core.all { it.mastery >= Mastery.FAMILIAR }
        // Recorded like any other lesson, at (mastery:<stageId>, 0). See LearningTree.masteryUnitId.
        val checkpointDone =
            progress[LearningTree.masteryUnitId(definition.id) to 0]?.isComplete == true
        val checkpoint = TreeNodeState(
            node = TreeNode.MasteryTest(definition.id),
            title = "Mastery",
            mastery = if (checkpointDone) Mastery.MASTERED else Mastery.NONE,
            isUnlocked = isSectionUnlocked && coreDone,
            isCurrent = isSectionUnlocked && coreDone && !checkpointDone && !currentMarked
        )

        return core + checkpoint + deepDive
    }

    /** The unit key a section draws on: its theme, or the closing remainder. */
    private fun unitIdsFor(definition: SectionDefinition): List<String> =
        when (val source = definition.source) {
            is SectionSource.Theme -> listOf(themeUnitId(source.tag))
            SectionSource.Remainder -> listOf(REMAINDER_UNIT_ID)
        }

    /** The words belonging to one lesson, in teaching order so lessons stay stable between runs. */
    suspend fun wordsFor(ref: LessonRef): List<VocabularyEntity> {
        LearningTree.sectionIdForMasteryUnit(ref.unitId)?.let { return masteryWordsFor(it) }

        val words = allWordsByUnit()[ref.unitId] ?: return emptyList()
        val range = LessonPlan.wordIndicesFor(ref.lessonIndex, words.size)
        if (range.isEmpty()) return emptyList()
        return words.slice(range)
    }

    /**
     * The words a stage's checkpoint tests: the ten from its core the learner knows least well.
     *
     * Drawn from the core tier only, because the checkpoint's job is to ask whether the stage was
     * learned, and the core is the part the stage actually asked of the learner — testing optional
     * deep-dive words nobody was told to do would make the checkpoint a trap.
     *
     * Weakest-first rather than at random. A checkpoint the learner passes by being shown the ten
     * words they already had cold measures nothing; the value of testing a whole stage days later is
     * that it finds what has decayed, so it should look there first. Ties keep teaching order, so a
     * run is stable between attempts rather than reshuffling under the learner.
     */
    private suspend fun masteryWordsFor(sectionId: String): List<VocabularyEntity> {
        val definition = LearningTree.sections.firstOrNull { it.id == sectionId } ?: return emptyList()
        val wordsByUnit = allWordsByUnit()
        val coreWordCount = LearningTree.CORE_LESSONS_PER_STAGE * LessonPlan.WORDS_PER_LESSON

        return unitIdsFor(definition)
            .flatMap { wordsByUnit[it].orEmpty() }
            .take(coreWordCount)
            .sortedBy { LearningTree.masteryOf(it).ordinal }
            .take(LearningTree.MASTERY_WORD_COUNT)
    }

    /**
     * The words a lesson actually practises: its own slice, plus a couple of older ones.
     *
     * Kept separate from [wordsFor], which stays the stable definition of what the lesson *is* — the
     * Learn screen counts it to say "Lesson 2 - 7 words", and that number should not move around
     * with the state of the review deck.
     */
    suspend fun practiceWordsFor(ref: LessonRef): List<VocabularyEntity> {
        val lessonWords = wordsFor(ref)
        if (lessonWords.isEmpty()) return lessonWords

        // A checkpoint tests its stage and nothing else. Interleaving exists to fold older words
        // into a lesson about new ones; folding another stage's leeches into this stage's checkpoint
        // would make a learner's result depend on words the stage never taught.
        if (LearningTree.sectionIdForMasteryUnit(ref.unitId) != null) return lessonWords

        val leeches = vocabularyRepository.getLeechWords(limit = Interleaving.REVISITED_PER_LESSON * 2)
        val due = vocabularyRepository.getDueReviewWordsStrict(limit = Interleaving.REVISITED_PER_LESSON * 4)
        return Interleaving.compose(lessonWords, leeches, due)
    }

    suspend fun exercisesFor(ref: LessonRef): List<Exercise> =
        exerciseGenerator.build(practiceWordsFor(ref))

    /**
     * The next lesson to do: the first incomplete lesson in the first incomplete unit.
     *
     * Returns null only when every lesson in the corpus is complete, which the Learn screen renders
     * as a genuine finished state rather than as an empty list.
     */
    suspend fun nextLesson(): LessonRef? {
        val completed = lessonDao.getAllOnce().filter { it.isComplete }
            .map { it.unitId to it.lessonIndex }.toSet()

        allWordsByUnit().forEach { (category, words) ->
            val lessonCount = LessonPlan.lessonCountFor(words.size)
            for (index in 0 until lessonCount) {
                if (category to index !in completed) return LessonRef(category, index)
            }
        }
        return null
    }

    suspend fun progressFor(ref: LessonRef): LessonProgressEntity? =
        lessonDao.get(ref.unitId, ref.lessonIndex)

    /**
     * Records a finished lesson and awards XP.
     *
     * [accuracy] is the fraction of exercises answered correctly on the *first* attempt, so retrying
     * until correct still finishes the lesson but does not earn the perfect bonus. Returns the XP
     * awarded so the completion screen can count it up.
     *
     * Best accuracy is kept rather than last, so replaying a lesson can only improve the record.
     */
    suspend fun completeLesson(ref: LessonRef, accuracy: Float): Int {
        val existing = lessonDao.get(ref.unitId, ref.lessonIndex)
        lessonDao.upsert(
            LessonProgressEntity(
                unitId = ref.unitId,
                lessonIndex = ref.lessonIndex,
                isComplete = true,
                bestAccuracy = maxOf(existing?.bestAccuracy ?: 0f, accuracy),
                timesCompleted = (existing?.timesCompleted ?: 0) + 1,
                lastCompletedAt = System.currentTimeMillis()
            )
        )

        val xp = LessonPlan.xpFor(accuracy)
        userProgressRepository.addXp(xp)
        userProgressRepository.recordLearningActivity()
        return xp
    }

    /** Lesson completions since [sinceEpochMillis], for the weekly strip and XP chart. */
    suspend fun completionsSince(sinceEpochMillis: Long): List<LessonProgressEntity> =
        lessonDao.completedSince(sinceEpochMillis)

    /** Merge point for cross-device sync, mirroring how game levels are reconciled. */
    suspend fun upsertAll(progress: List<LessonProgressEntity>) = lessonDao.upsertAll(progress)

    suspend fun allProgressOnce(): List<LessonProgressEntity> = lessonDao.getAllOnce()

    /**
     * Groups the corpus by category, preserving the order words come back in.
     *
     * Read fresh on each call rather than cached: the realtime Firestore listener can add words while
     * the app is open, and a stale unit list would silently hide the new content.
     */
    /**
     * The corpus grouped by the unit key a lesson is recorded against.
     *
     * Grouped by `theme`, not by the dictionary's `category`. The categories are loose import bins -
     * "Greetings & Essentials" holds *adëg* (behind) and *at* (and) - so a section built on them
     * teaches something other than its title, which is the defect this grouping exists to fix.
     *
     * Two kinds of word end up in the remainder unit, and both would otherwise be unreachable: one
     * that carries no theme, and one whose theme is too thin to ship as a section. A word the corpus
     * records but no section teaches has been quietly hidden from the learner, which is the opposite
     * of what an app for an endangered language is for.
     *
     * Read fresh on each call rather than cached: the realtime Firestore listener can add words while
     * the app is open, and a stale unit list would silently hide the new content.
     */
    private suspend fun allWordsByUnit(): Map<String, List<VocabularyEntity>> {
        val all = vocabularyRepository.getAllVocabularyOnce()

        val byTheme = all
            .filter { it.theme.isNotBlank() }
            .groupBy { it.theme.trim().lowercase() }

        val shippingThemes = LearningTree.sections
            .mapNotNull { (it.source as? SectionSource.Theme)?.tag }
            .filter { LearningTree.isViable(byTheme[it].orEmpty().size) }
            .toSet()

        val units = mutableMapOf<String, List<VocabularyEntity>>()
        shippingThemes.forEach { tag ->
            units[themeUnitId(tag)] = LearningTree.teachingOrder(byTheme[tag].orEmpty())
        }

        val remainder = all.filter {
            val tag = it.theme.trim().lowercase()
            tag.isBlank() || tag !in shippingThemes
        }
        units[REMAINDER_UNIT_ID] = LearningTree.teachingOrder(remainder)

        return units
    }

    companion object {
        /** Kept as delegates so the key format has exactly one definition, in [LearningTree]. */
        fun themeUnitId(tag: String): String = LearningTree.themeUnitId(tag)

        const val REMAINDER_UNIT_ID = LearningTree.REMAINDER_UNIT_ID
    }
}
