package com.kasiguru.domain.lesson

import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.util.RecallPrompt

/**
 * One step in a lesson.
 *
 * The variants mirror the question shapes the six existing mini-games already produce, so a lesson is
 * built from mechanics the app has shipped and tested rather than from new ones. What the lesson layer
 * adds is sequence, feedback and consequence — the things that were missing.
 *
 * Every variant carries its [word] so the completion screen can list what was covered and the
 * feedback panel can teach from the full entry rather than from the answer string alone.
 */
sealed interface Exercise {

    val word: VocabularyEntity
    val options: List<String>
    val answer: String

    /** The instruction shown above the prompt. */
    val instruction: String

    /**
     * Kasiguranin shown, learner picks the Tagalog or English meaning — or the reverse.
     * Mirrors the Word Match and Reverse Match games.
     */
    data class ChooseTranslation(
        override val word: VocabularyEntity,
        override val options: List<String>,
        override val answer: String,
        /** True when the prompt is the Kasiguranin headword. */
        val promptIsKasiguranin: Boolean
    ) : Exercise {
        /**
         * What the learner reads.
         *
         * The meaning direction cannot simply print `tagalog`: for a borrowed or shared word the
         * Tagalog gloss *is* the headword (`buhay` glosses as `buhay`), so the prompt would sit
         * above an option list containing itself and the exercise would test nothing. The same rule
         * that protects typed recall protects this. The fallback is unreachable in practice --
         * ExerciseGenerator flips such a word to the other direction -- and exists so this getter is
         * total rather than throwing on a corpus entry with no usable gloss at all.
         */
        val prompt: String
            get() = if (promptIsKasiguranin) {
                word.kasiguranin
            } else {
                RecallPrompt.meaningFor(word.kasiguranin, word.tagalog, word.english) ?: word.tagalog
            }
        override val instruction: String
            get() = if (promptIsKasiguranin) "What does this mean?" else "Say this in Kasiguranin"
    }

    /**
     * Audio plays, learner picks the written word.
     *
     * Only generated for entries that actually carry a recording, and no entry in the corpus does,
     * so nothing generates this today. It is kept for the day recordings exist — the mini-game that
     * used to lean on the same missing audio is now [TypeWord]-based Word Recall instead.
     */
    data class ListenAndChoose(
        override val word: VocabularyEntity,
        override val options: List<String>,
        override val answer: String
    ) : Exercise {
        override val instruction: String get() = "Which word did you hear?"
    }

    /**
     * The word is blanked out of its own example sentence. Mirrors Fill in the Blank, and is the only
     * exercise that shows the word doing a job in a real sentence.
     */
    data class FillBlank(
        override val word: VocabularyEntity,
        override val options: List<String>,
        override val answer: String,
        val sentenceWithBlank: String,
        val translation: String
    ) : Exercise {
        override val instruction: String get() = "Complete the sentence"
    }

    /**
     * Pick the right aspectual form of a verb. Mirrors the Aspect Builder.
     *
     * This is the exercise most specific to Kasiguranin: the language marks neutral, imperfective,
     * perfective and contemplative aspect, and those four forms are already stored on every verb entry
     * but are currently buried in a collapsed row nobody opens.
     */
    data class ChooseAspect(
        override val word: VocabularyEntity,
        override val options: List<String>,
        override val answer: String,
        val aspectLabel: String
    ) : Exercise {
        override val instruction: String get() = "Choose the $aspectLabel form"
    }

    /**
     * The meaning is shown and the learner types the Kasiguranin word from memory.
     *
     * The only exercise in the app that asks for *production* rather than recognition. Picking the
     * right option out of four is a far weaker test than producing the word unaided — recognition
     * can succeed on a familiarity signal that is nowhere near strong enough to use the word — so
     * every other exercise systematically overstates what the learner knows.
     *
     * It also has no data requirements beyond a headword and a meaning, which makes it the natural
     * fallback for entries too sparse to support any other second-pass shape: no example sentence,
     * too few aspect forms, and no audio anywhere in the corpus. Those words previously got one
     * exercise and were never revisited.
     *
     * [options] is empty — the answer is typed, and grading goes through
     * [com.kasiguru.util.RecallAnswerMatcher] rather than string equality so keyboard limitations
     * (no schwa key) and stress accents do not read as wrong answers.
     */
    data class TypeWord(
        override val word: VocabularyEntity,
        override val answer: String,
        /** The meaning shown as the prompt — Tagalog when present, otherwise English. */
        val promptMeaning: String
    ) : Exercise {
        override val options: List<String> = emptyList()
        override val instruction: String get() = "Type this in Kasiguranin"
    }

    /**
     * Build the sentence: the meaning is given, and the learner taps Kasiguranin word chips into
     * order.
     *
     * The shape a learner recognises from Duolingo, and the only one in this lesson system that
     * tests word order rather than vocabulary alone. It is built strictly from a sentence somebody
     * actually recorded on the word ([VocabularyEntity.exampleSentence]) - never from a sentence
     * assembled here. Inventing a Kasiguranin sentence to fill a lesson would put fabricated data
     * into the record this app exists to preserve.
     *
     * Because of that rule this exercise is dormant on a corpus without sentences, and switches on
     * for a word the moment one is authored in the admin portal. [options] is the shuffled chip
     * bank: the sentence's own words plus a few plausible intruders.
     */
    data class SentenceBuild(
        override val word: VocabularyEntity,
        override val options: List<String>,
        /** The sentence, exactly as recorded, which is what a correct arrangement must equal. */
        override val answer: String,
        /** The meaning shown as the prompt - the recorded translation of [answer]. */
        val translation: String,
        /** The words of [answer] in their correct order, for grading and for the reveal. */
        val correctOrder: List<String>
    ) : Exercise {
        override val instruction: String get() = "Build the sentence"
    }

    /**
     * Match each Kasiguranin word to its meaning.
     *
     * The one new shape that needs no data beyond a headword and a gloss, which is why it matters
     * here: with no audio in the corpus, five example sentences and eight sets of aspect forms,
     * every other alternative shape stays dormant and a lesson falls through to multiple choice and
     * typing on repeat. This one always fires.
     *
     * [pairs] is authored order - Kasiguranin to meaning; the screen shuffles each column
     * independently. [word] is the first pair's word, carried only to satisfy the interface and to
     * give the completion screen something to list.
     */
    data class MatchPairs(
        override val word: VocabularyEntity,
        val pairs: List<Pair<VocabularyEntity, String>>
    ) : Exercise {
        override val options: List<String> = pairs.map { it.second }
        override val answer: String = pairs.joinToString("|") { "${it.first.kasiguranin}=${it.second}" }
        override val instruction: String get() = "Match each word to its meaning"
    }
}
