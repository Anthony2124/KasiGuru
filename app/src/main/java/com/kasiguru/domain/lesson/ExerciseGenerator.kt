package com.kasiguru.domain.lesson

import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.util.RecallPrompt
import com.kasiguru.util.srs.Sm2Algorithm
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a lesson's word list into an ordered run of exercises.
 *
 * Distractors come from [VocabularyRepository.getDistractorsForWord], which already prefers words from
 * the same category and then words of similar length or first letter. That matters pedagogically: a
 * multiple choice between "ant" and "helicopter" tests nothing, while a choice between two same-category
 * words the learner keeps confusing is where the learning happens. Reusing it also means the lesson
 * layer and the mini-games stay consistent about what counts as a plausible wrong answer.
 */
@Singleton
class ExerciseGenerator @Inject constructor(
    private val vocabularyRepository: VocabularyRepository
) {

    /**
     * Builds a lesson.
     *
     * Each word gets one exercise chosen from the shapes its own data can support, then the strongest
     * candidates are revisited in a second, different shape until the run reaches
     * [LessonPlan.EXERCISES_PER_LESSON]. Re-testing a word in a different modality within the same
     * session is the point — recognising a written word does not mean you can hear it.
     */
    suspend fun build(words: List<VocabularyEntity>): List<Exercise> {
        if (words.isEmpty()) return emptyList()

        val exercises = mutableListOf<Exercise>()

        // First pass: introduce every word once.
        words.forEachIndexed { index, word ->
            // Alternate the translation direction so a run never becomes a single drill.
            exercises += primaryExercise(word, preferKasiguraninPrompt = index % 2 == 0)
        }

        // One matching round per lesson, placed after the introductions so every word in it has been
        // met once. It is here because it is the only alternative shape this corpus can always feed:
        // with no audio, five example sentences and eight sets of aspect forms in 1,246 words, every
        // other second-pass shape is usually dormant and a run collapses into multiple choice and
        // typing on repeat.
        matchPairsExercise(words)?.let { exercises += it }

        // Second pass: revisit words in a different shape until the run is long enough.
        var cursor = 0
        while (exercises.size < LessonPlan.EXERCISES_PER_LESSON && cursor < words.size) {
            val word = words[cursor]
            secondaryExercise(word, exercises)?.let { exercises += it }
            cursor++
        }

        return exercises
    }

    private suspend fun primaryExercise(
        word: VocabularyEntity,
        preferKasiguraninPrompt: Boolean
    ): Exercise {
        // A leech always gets the recognition direction, whatever the alternation says. Reading a
        // Kasiguranin word and picking its meaning is the easiest retrieval the app has, and a word
        // forgotten five times needs to be met successfully before it can be tested harder.
        //
        // A word with no gloss that differs from its headword is also forced into this direction:
        // asking "say this in Kasiguranin" above the prompt `buhay` when the answer is `buhay` hands
        // the answer over, and roughly a tenth of the corpus glosses to itself in Tagalog.
        val hasUsableMeaning =
            RecallPrompt.meaningFor(word.kasiguranin, word.tagalog, word.english) != null
        val kasiguraninPrompt = preferKasiguraninPrompt || Sm2Algorithm.isLeech(word) || !hasUsableMeaning
        val answer = if (kasiguraninPrompt) meaningOf(word) else word.kasiguranin
        val distractors = distractorStrings(word, kasiguraninAnswers = !kasiguraninPrompt)
        return Exercise.ChooseTranslation(
            word = word,
            options = (distractors + answer).distinct().shuffled(),
            answer = answer,
            promptIsKasiguranin = kasiguraninPrompt
        )
    }

    /**
     * A second look at [word] in a shape not already used for it, or null when the entry lacks the
     * data any alternative shape needs. Never invents content: a word with no audio never becomes a
     * listening exercise, and a word with no example sentence never becomes a fill-in-the-blank.
     */
    private suspend fun secondaryExercise(
        word: VocabularyEntity,
        existing: List<Exercise>
    ): Exercise? {
        val alreadyUsed = existing.filter { it.word.id == word.id }.map { it::class }

        if (word.audioFileName.isNotBlank() && Exercise.ListenAndChoose::class !in alreadyUsed) {
            val distractors = distractorStrings(word, kasiguraninAnswers = true)
            return Exercise.ListenAndChoose(
                word = word,
                options = (distractors + word.kasiguranin).distinct().shuffled(),
                answer = word.kasiguranin
            )
        }

        if (Exercise.SentenceBuild::class !in alreadyUsed) {
            sentenceBuildExercise(word)?.let { return it }
        }

        val sentence = word.exampleSentence
        if (sentence.isNotBlank() &&
            sentence.contains(word.kasiguranin, ignoreCase = true) &&
            Exercise.FillBlank::class !in alreadyUsed
        ) {
            val distractors = distractorStrings(word, kasiguraninAnswers = true)
            return Exercise.FillBlank(
                word = word,
                options = (distractors + word.kasiguranin).distinct().shuffled(),
                answer = word.kasiguranin,
                sentenceWithBlank = sentence.replace(word.kasiguranin, BLANK, ignoreCase = true),
                translation = word.exampleTranslation
            )
        }

        val aspects = aspectFormsOf(word)
        if (aspects.size >= 3 && Exercise.ChooseAspect::class !in alreadyUsed) {
            val (label, correct) = aspects.entries.random().toPair()
            return Exercise.ChooseAspect(
                word = word,
                options = aspects.values.distinct().shuffled(),
                answer = correct,
                aspectLabel = label
            )
        }

        // Typed recall needs nothing but a headword and a meaning, so it catches every entry the
        // shapes above cannot serve — no example sentence, fewer than three aspect forms, and (for
        // the whole corpus) no audio. Those words used to appear once in pass one and never again,
        // which is the weakest possible treatment for exactly the sparsest entries.
        // Not simply "Tagalog, or English if blank": a gloss identical to the headword would print
        // the answer directly above the input. See RecallPrompt.
        // Not for a leech: typing a word from memory is the hardest retrieval in the app, and this
        // is the word the learner has already lost five times. It gets a second look only in a shape
        // that shows it -- the fill-in-the-blank above -- or no second look at all.
        val meaning = RecallPrompt.meaningFor(word.kasiguranin, word.tagalog, word.english)
        if (meaning != null && !Sm2Algorithm.isLeech(word) && Exercise.TypeWord::class !in alreadyUsed) {
            return Exercise.TypeWord(
                word = word,
                answer = word.kasiguranin,
                promptMeaning = meaning
            )
        }

        return null
    }

    /**
     * A matching round over the lesson's own words, or null when too few of them carry a usable gloss.
     *
     * Capped at [MATCH_PAIRS_SIZE]: more than that and the exercise stops being a quick check and
     * becomes a puzzle, on a screen that has to fit two columns side by side on a small phone.
     */
    private fun matchPairsExercise(words: List<VocabularyEntity>): Exercise? {
        val usable = words
            .filter { RecallPrompt.meaningFor(it.kasiguranin, it.tagalog, it.english) != null }
            .distinctBy { it.kasiguranin.lowercase() }
            .take(MATCH_PAIRS_SIZE)
        if (usable.size < MATCH_PAIRS_MIN) return null

        return Exercise.MatchPairs(
            word = usable.first(),
            pairs = usable.map { it to meaningOf(it) }
        )
    }

    /**
     * Build-the-sentence for [word], or null when the word carries no sentence worth building.
     *
     * The sentence has to be one somebody recorded on the word. Assembling one here from the
     * corpus - stringing plausible words together - would put invented Kasiguranin in front of a
     * learner and, worse, into a thesis artifact, so a word without a real sentence simply does not
     * get this shape. Today that is nearly the whole corpus; each sentence authored in the admin
     * portal switches it on for one more word, with no app release.
     */
    private suspend fun sentenceBuildExercise(word: VocabularyEntity): Exercise? {
        // Two sources, both authored by people: a sentence recorded on this very word, and the
        // project's own sentence bank. Never a sentence assembled here out of corpus words -- that
        // would be invented Kasiguranin in front of a learner and inside a thesis artifact.
        val ownSentence = word.exampleSentence.trim()
        val (parts, translation) = when {
            ownSentence.isNotBlank() && word.exampleTranslation.isNotBlank() ->
                ownSentence.split(" ").filter { it.isNotBlank() } to word.exampleTranslation

            else -> {
                val authored = SentenceBank.sentenceUsing(word.kasiguranin) ?: return null
                authored.kasiguranin to authored.english
            }
        }
        if (parts.size < SENTENCE_BUILD_MIN_WORDS) return null

        // Intruder chips, so arranging the bank is a choice rather than a sort. Drawn from the same
        // distractor pool the multiple choice uses, and never a word already in the sentence.
        val inSentence = parts.map { SentenceBank.normalise(it) }.toSet()
        val intruders = vocabularyRepository.getDistractorsForWord(word, count = 4)
            .map { it.kasiguranin }
            .filter { it.isNotBlank() && SentenceBank.normalise(it) !in inSentence }
            .take(SENTENCE_BUILD_INTRUDERS)

        return Exercise.SentenceBuild(
            word = word,
            options = (parts + intruders).shuffled(),
            answer = parts.joinToString(" "),
            translation = translation,
            correctOrder = parts
        )
    }

    /**
     * How a word's meaning is written on an answer button.
     *
     * Tagalog and English together, because the audience reads both and one gloss alone is often
     * ambiguous — except when the Tagalog is the same string as the Kasiguranin headword, which is
     * common (`sayaw` is "sayaw · dance"). Printing it then hands the learner the answer: the option
     * that repeats the prompt is obviously the right one, and the exercise tests nothing. In that case
     * only the English is shown.
     */
    private fun meaningOf(word: VocabularyEntity): String {
        val tagalogRepeatsHeadword = word.tagalog.equals(word.kasiguranin, ignoreCase = true)
        return when {
            tagalogRepeatsHeadword && word.english.isNotBlank() -> word.english
            word.tagalog.isNotBlank() && word.english.isNotBlank() -> "${word.tagalog} · ${word.english}"
            word.tagalog.isNotBlank() -> word.tagalog
            else -> word.english
        }
    }

    private suspend fun distractorStrings(
        word: VocabularyEntity,
        kasiguraninAnswers: Boolean
    ): List<String> =
        vocabularyRepository.getDistractorsForWord(word, count = 3)
            .map { if (kasiguraninAnswers) it.kasiguranin else meaningOf(it) }
            .filter { it.isNotBlank() }

    private fun aspectFormsOf(word: VocabularyEntity): Map<String, String> = buildMap {
        if (word.neutralForm.isNotBlank()) put("neutral", word.neutralForm)
        if (word.imperfectiveForm.isNotBlank()) put("imperfective", word.imperfectiveForm)
        if (word.perfectiveForm.isNotBlank()) put("perfective", word.perfectiveForm)
        if (word.contemplativeForm.isNotBlank()) put("contemplative", word.contemplativeForm)
    }

    private companion object {
        const val BLANK = "____"

        /** Pairs in one matching round. */
        const val MATCH_PAIRS_SIZE = 4

        /** Below this a matching round is not worth the screen it takes. */
        const val MATCH_PAIRS_MIN = 3

        /** A "sentence" of two words is a phrase; arranging it tests nothing. */
        const val SENTENCE_BUILD_MIN_WORDS = 3

        /** Wrong chips offered beside the sentence's own words. */
        const val SENTENCE_BUILD_INTRUDERS = 2
    }
}
