package com.kasiguru.domain.lesson

import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.util.srs.Sm2Algorithm

/**
 * The learning tree: the corpus arranged as a journey through Casiguran.
 *
 * Nothing here authors content. A section is a view over vocabulary the dictionary already holds, and
 * a lesson node is a [LessonPlan] slice of it, so the tree adds a route through the corpus rather than
 * a second copy of it. That is deliberate: the admin portal can add words without a schema migration,
 * and a learner's history stays attached to `(unitId, lessonIndex)` however the tree above it changes.
 *
 * The one rule that governs the section list: **a section ships when real words back it.** Sections
 * below [MIN_WORDS_FOR_SECTION] are dropped rather than rendered empty, because the alternative — a
 * node with nothing behind it — invites filling the gap with invented Kasiguranin, and this corpus is
 * the primary research record of an endangered language.
 */
object LearningTree {

    /**
     * The floor for a section to appear at all.
     *
     * Five lesson nodes at [LessonPlan.WORDS_PER_LESSON] words each. Below that a "section" is one or
     * two lessons and a mastery test, which reads as a stub rather than a stage of a journey.
     */
    const val MIN_WORDS_FOR_SECTION = 35

    /**
     * Lessons in a stage's core tier — the part every learner is expected to walk.
     *
     * A stage carries whatever the corpus gives it, and the corpus is lopsided: the describing words
     * run to 140 and the household to 50. Teaching all of both in sequence made the tree roughly 180
     * nodes long, and a path that takes a hundred sittings to finish is one nobody finishes; the
     * learner who quits in the middle of lesson 19 of "Paglalarawan" has not been taught more than
     * the one who finished a shorter stage, they have just been marched further.
     *
     * So a stage is two tiers. Six lessons — 42 words, about a quarter of an hour — is the core, and
     * it is what the gate to the next stage is measured against. Everything past it is a deep dive:
     * the same lessons, the same `(unitId, lessonIndex)` identity, drawn after the checkpoint and
     * optional. Nothing is dropped from the corpus; the learner is simply no longer required to walk
     * all of it in one line before seeing the next stage.
     */
    const val CORE_LESSONS_PER_STAGE = 6

    /** Fraction of a section's lesson XP that opens the next one. */
    const val GATE_FRACTION = 0.6f

    /**
     * Ceiling on a section's gate, in XP.
     *
     * Without it the largest section sets the hardest gate: the remainder section carries hundreds of
     * words, so 60% of its lesson XP would stand as several hundred XP between the learner and
     * everything after it. The cap keeps a big section from becoming a wall purely because a lot of
     * words happen to land in it.
     */
    const val GATE_XP_CAP = 300

    /** Share of a node's words at Practicing or better for the node itself to read as Practicing. */
    private const val PRACTICING_SHARE = 0.5f

    /** Share of a node's words retained for the node to read as Mastered. */
    private const val MASTERED_SHARE = 0.8f

    /**
     * The sections, in the order the learner walks them.
     *
     * Ordered as a journey rather than by size: you greet someone, meet their household, step inside
     * it, share a meal, then work outward to the body, the animals, the land, and finally to
     * counting, describing, doing and feeling.
     *
     * Every section draws on a `theme`, never on the dictionary's `category`. A section whose theme
     * is too thin to walk through does not ship, and its words fall to [SectionSource.Remainder] so
     * they are still taught somewhere. See learning_tree.md for the measured counts.
     */
    val sections: List<SectionDefinition> = listOf(
        SectionDefinition("pagbati", "Pagbati at Sarili", "Greetings, courtesy and how you feel",
            "Say hello, and be understood", SectionSource.Theme("pagbati")),
        SectionDefinition("pamilya", "Pamilya at Mga Tao", "Family and people",
            "Meet the household", SectionSource.Theme("pamilya")),
        SectionDefinition("tahanan", "Tahanan", "The house and what is in it",
            "Step inside a Casiguran home", SectionSource.Theme("tahanan")),
        SectionDefinition("pagkain", "Pagkain at Kainan", "Food and eating",
            "Share a meal", SectionSource.Theme("pagkain")),
        SectionDefinition("paglalakbay", "Paglalakbay at Direksyon", "Travel, direction and position",
            "Find your way, and say where things are", SectionSource.Theme("paglalakbay")),
        SectionDefinition("katawan", "Katawan at Kalusugan", "The body and health",
            "Say how you feel, and where it hurts", SectionSource.Theme("katawan")),
        SectionDefinition("kalikasan", "Kalikasan at Panahon", "Land, sea, sky and weather",
            "Read the land, the sea and the sky", SectionSource.Theme("kalikasan")),
        SectionDefinition("hayop", "Mga Hayop", "Animals and wildlife",
            "Name what lives here", SectionSource.Theme("hayop")),
        SectionDefinition("bilang", "Bilang at Oras", "Numbers and time",
            "Count, and tell the time", SectionSource.Theme("bilang")),
        SectionDefinition("kabuhayan", "Kabuhayan", "Work, tools and livelihood",
            "Learn how people live", SectionSource.Theme("kabuhayan")),
        SectionDefinition("kilos", "Mga Kilos", "Actions and doing",
            "Say what people do", SectionSource.Theme("kilos")),
        SectionDefinition("paglalarawan", "Paglalarawan", "Describing words",
            "Describe what you see", SectionSource.Theme("paglalarawan")),
        SectionDefinition("araw_araw", "Pang-araw-araw", "Everything else people say",
            "Everything else people actually say", SectionSource.Remainder)
    )

    /**
     * The order a section teaches its words in.
     *
     * Not alphabetical. Slicing a section alphabetically made lesson one of every section a run of
     * a-words -- *addyëk*, *adëg*, *agton*, *aneno* -- which is an accident of spelling, not a first
     * lesson. Concrete nouns come first because they are the easiest thing to picture and the
     * easiest to test with a picture-free multiple choice; verbs follow; the describing words and
     * everything else come last, since they mostly modify things the learner now has names for.
     *
     * Alphabetical *within* each band, so the order is stable between runs. [LessonPlan] slices by
     * position, and a lesson that reshuffles on every launch would detach a learner's progress from
     * the words they actually studied.
     */
    fun teachingOrder(words: List<VocabularyEntity>): List<VocabularyEntity> =
        words.sortedWith(compareBy({ posBand(it.partOfSpeech) }, { it.kasiguranin.lowercase() }))

    /** Lower sorts earlier. Anything unrecognised sits with the tail rather than jumping the queue. */
    private fun posBand(partOfSpeech: String): Int = when (partOfSpeech.trim().lowercase()) {
        "noun" -> 0
        "verb" -> 1
        "adjective" -> 2
        "adverb" -> 3
        else -> 4
    }

    /**
     * Unit key for the closing section.
     *
     * Namespaced like a theme so the two can never collide, and fixed, because it is written into
     * `lesson_progress.unitId` the moment a learner finishes one of its lessons.
     */
    const val REMAINDER_UNIT_ID = "theme:_remainder"

    /** Unit key for a theme-sourced section. */
    fun themeUnitId(tag: String): String = "theme:${tag.trim().lowercase()}"

    /**
     * Words a stage's checkpoint tests.
     *
     * Ten, not the whole core. [ExerciseGenerator] introduces every word it is given, so handing it
     * a stage's forty-two core words would build a forty-two exercise run — a quarter of an hour of
     * uninterrupted testing at the end of every stage, which is how a checkpoint becomes the thing
     * learners quit at.
     */
    const val MASTERY_WORD_COUNT = 10

    /**
     * Namespace for a checkpoint's progress row.
     *
     * A checkpoint is recorded exactly like a lesson — one `lesson_progress` row at
     * `(mastery:<stageId>, 0)`. That is why the checkpoint needs no table, no migration, no second
     * player screen and no route of its own: it *is* a lesson, over a different set of words. The
     * prefix keeps it from ever colliding with a theme unit key.
     */
    const val MASTERY_UNIT_PREFIX = "mastery:"

    /** The unit key a stage's checkpoint records against. */
    fun masteryUnitId(sectionId: String): String = "$MASTERY_UNIT_PREFIX$sectionId"

    /** The stage a checkpoint unit key belongs to, or null when [unitId] is not a checkpoint. */
    fun sectionIdForMasteryUnit(unitId: String): String? =
        if (unitId.startsWith(MASTERY_UNIT_PREFIX)) {
            unitId.removePrefix(MASTERY_UNIT_PREFIX).takeIf { it.isNotBlank() }
        } else {
            null
        }

    /** The unit key a section's lessons are recorded against. */
    fun unitIdFor(source: SectionSource): String = when (source) {
        is SectionSource.Theme -> themeUnitId(source.tag)
        SectionSource.Remainder -> REMAINDER_UNIT_ID
    }

    /**
     * The section a unit key belongs to, or null when it belongs to none.
     *
     * Exists because a unit key is a storage detail and must never reach a learner. It did: Today's
     * Path printed `theme:pamilya` as a lesson's title, which is the internal key for "Pamilya at
     * Mga Tao". Anything showing a lesson to a person resolves the key through here first.
     */
    fun sectionForUnit(unitId: String): SectionDefinition? {
        // A checkpoint's unit key resolves to the stage it tests, so "mastery:pagbati" reads as
        // "Pagbati at Sarili" wherever a lesson's stage is shown. Without this the key itself
        // reaches the learner, which is the bug that once printed "theme:pamilya" as a title.
        sectionIdForMasteryUnit(unitId)?.let { sectionId ->
            return sections.firstOrNull { it.id.equals(sectionId, ignoreCase = true) }
        }
        return sections.firstOrNull { unitIdFor(it.source).equals(unitId, ignoreCase = true) }
    }

    /** True when [wordCount] is enough for the section to be worth walking through. */
    fun isViable(wordCount: Int): Boolean = wordCount >= MIN_WORDS_FOR_SECTION

    /**
     * XP that must be earned inside a section before the next one opens.
     *
     * Derived from the section's own size so a short section is not gated like a long one, then
     * capped - see [GATE_XP_CAP].
     */
    fun requiredXpToOpenNext(lessonNodeCount: Int): Int {
        val available = lessonNodeCount * LessonPlan.XP_PER_LESSON
        return minOf((available * GATE_FRACTION).toInt(), GATE_XP_CAP)
    }

    /**
     * How well one word is known, read from the SM-2 state it already carries.
     *
     * [VocabularyEntity.isLearned] is the authority for [Mastery.MASTERED] rather than a count of
     * correct answers: it already demands three correct reviews, no active relearning step, and an
     * interval of at least six days, precisely because two lucky answers to a four-option question
     * land about 6% of the time. The tree inherits that bar instead of setting a friendlier one.
     */
    fun masteryOf(word: VocabularyEntity): Mastery = when {
        word.isLearned -> Mastery.MASTERED
        word.timesReviewed >= Sm2Algorithm.MIN_LEARNED_REVIEWS -> Mastery.PRACTICING
        word.timesReviewed >= 1 -> Mastery.FAMILIAR
        else -> Mastery.NONE
    }

    /**
     * How a lesson node reads, given whether its lesson is finished and how its words are doing.
     *
     * Completing the lesson earns [Mastery.FAMILIAR] outright - the learner has met all seven words.
     * The tiers above it are earned by coming back, which is the behaviour the tree exists to
     * encourage, so they are measured across the node's words rather than from the lesson result.
     */
    fun nodeMastery(isLessonComplete: Boolean, words: List<VocabularyEntity>): Mastery {
        if (!isLessonComplete) return Mastery.NONE
        if (words.isEmpty()) return Mastery.FAMILIAR

        val mastered = words.count { it.isLearned }
        if (mastered >= (words.size * MASTERED_SHARE)) return Mastery.MASTERED

        val practising = words.count { masteryOf(it) >= Mastery.PRACTICING }
        if (practising >= (words.size * PRACTICING_SHARE)) return Mastery.PRACTICING

        return Mastery.FAMILIAR
    }
}

/**
 * How well something is known. Ordered, so `>=` comparisons read the way they sound.
 *
 * The three named tiers are the learner-facing 🌱 Familiar / 🌿 Practicing / 🌳 Mastered.
 */
enum class Mastery { NONE, FAMILIAR, PRACTICING, MASTERED }

/**
 * Where a section's words come from.
 *
 * Not the dictionary's `category`. That field is a set of loose import bins - "Greetings &
 * Essentials" holds *adëg* (behind), *at* (and) and *attëd* (give) - so a section built on it
 * promises a subject its words never deliver, which is exactly what the first learner to walk the
 * path noticed. Sections are built on `theme` instead: a second, journey-facing home for a word,
 * proposed in bulk by `functions/tag_themes.js` and corrected by hand in the admin portal.
 */
sealed interface SectionSource {
    /** A `theme` tag, as written on the word. */
    data class Theme(val tag: String) : SectionSource

    /**
     * Everything no shipping section claims.
     *
     * Two kinds of word land here, and both would otherwise become unreachable: one the tagger could
     * not place, and one tagged to a theme too thin to ship as its own section. A corpus word that
     * no section teaches is a word the app has quietly hidden from the learner.
     */
    data object Remainder : SectionSource
}

/** A section of the tree as authored here, before any learner state is applied. */
data class SectionDefinition(
    val id: String,
    /** Tagalog title, as the learner sees it. */
    val title: String,
    /** English gloss, for accessibility and for learners who need it. */
    val gloss: String,
    /** One line naming the moment in the journey, shown under the title. */
    val journeyLine: String,
    val source: SectionSource
)

/** What kind of stop on the path a node is. */
sealed interface TreeNode {

    /** Seven new words. The only node type that introduces vocabulary. */
    data class Lesson(val ref: LessonRef, val positionInSection: Int) : TreeNode

    /** The section's own words, re-tested. Opens once every lesson in the section is complete. */
    data class MasteryTest(val sectionId: String) : TreeNode
}

/** A node with the learner's state applied. */
data class TreeNodeState(
    val node: TreeNode,
    val title: String,
    val mastery: Mastery,
    val isUnlocked: Boolean,
    /** True for the single node the learner should tap next. */
    val isCurrent: Boolean,
    /**
     * True for a lesson past the stage's core tier: still taught, no longer required.
     *
     * See [LearningTree.CORE_LESSONS_PER_STAGE]. A deep-dive node is drawn after the checkpoint and
     * does not count toward the gate, so a learner can move on to the next stage and come back for
     * the rest of a large stage's words later.
     */
    val isDeepDive: Boolean = false
)

/** A section with the learner's state applied. */
data class TreeSection(
    val definition: SectionDefinition,
    val wordCount: Int,
    val nodes: List<TreeNodeState>,
    /** XP earned from this section's completed lessons. */
    val earnedXp: Int,
    /** XP needed inside this section before the next opens. */
    val requiredXp: Int,
    val isUnlocked: Boolean
) {
    val id: String get() = definition.id

    /** Lesson nodes only - the mastery test is not a lesson and does not count toward the gate. */
    val lessonNodeCount: Int get() = nodes.count { it.node is TreeNode.Lesson }

    /** The lessons a learner is actually asked to walk. See [LearningTree.CORE_LESSONS_PER_STAGE]. */
    val coreLessonNodeCount: Int get() = nodes.count { it.node is TreeNode.Lesson && !it.isDeepDive }

    /** Lessons past the core tier: optional, and drawn after the checkpoint. */
    val deepDiveNodeCount: Int get() = nodes.count { it.isDeepDive }

    /**
     * Complete when the core is complete.
     *
     * Deep-dive lessons are deliberately excluded. A stage carrying 140 words would otherwise never
     * read as finished for any learner who moved on after its core, which is exactly the behaviour
     * the two tiers exist to permit.
     */
    val isComplete: Boolean
        get() {
            val core = nodes.filter { !it.isDeepDive }
            return core.isNotEmpty() && core.all { it.mastery >= Mastery.FAMILIAR }
        }

    /** Whether this section has been worked hard enough to open the next one. */
    val opensNext: Boolean get() = earnedXp >= requiredXp

    val gateFraction: Float
        get() = if (requiredXp <= 0) 1f else (earnedXp.toFloat() / requiredXp).coerceIn(0f, 1f)
}
