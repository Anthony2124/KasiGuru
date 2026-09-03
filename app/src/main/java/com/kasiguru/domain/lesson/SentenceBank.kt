package com.kasiguru.domain.lesson

/**
 * The project's authored Kasiguranin sentences.
 *
 * Fifteen sentences with English translations, written for this project rather than generated. They
 * are the only running Kasiguranin the app holds: the vocabulary corpus carries five example
 * sentences across 1,246 entries, and every seeded story page leaves its `kasiguranin` field blank
 * on purpose. Any exercise that asks a learner to arrange Kasiguranin words has to come from here or
 * from a sentence recorded on a word - never from words strung together at runtime, which would put
 * invented language into the record this app exists to preserve.
 *
 * They lived inside `SentenceOrderViewModel` as a literal, which meant the lesson system could not
 * reach them and a correction had to be made twice. One copy, two readers.
 */
object SentenceBank {

    /**
     * The shortest sentence worth asking a learner to arrange.
     *
     * A "sentence" of two words is a phrase, and putting it in order tests nothing. It lives here
     * rather than beside either of its readers because both the lesson's sentence-building exercise
     * and the sentence-order game apply the same floor to the same sentences, and the two drifting
     * apart would mean a sentence the game offers but a lesson silently refuses.
     */
    const val MIN_WORDS = 3

    val sentences: List<AuthoredSentence> = listOf(
    AuthoredSentence(
        kasiguranin = listOf("Magandang", "aldaw", "ha", "iyo", "'ttanan!"),
        english = "Good day to you all!"
    ),
    AuthoredSentence(
        kasiguranin = listOf("Kumusta", "na", "ing", "buhay", "mo?"),
        english = "How is your life?"
    ),
    AuthoredSentence(
        kasiguranin = listOf("Tinumáknəg", "ang", "anák."),
        english = "The child stood up."
    ),
    AuthoredSentence(
        kasiguranin = listOf("Maglákad", "akú", "niiláw."),
        english = "I will leave tomorrow."
    ),
    AuthoredSentence(
        kasiguranin = listOf("Bəbbi", "ang", "anak", "ni", "Kendy."),
        english = "Kendy's child is a girl."
    ),
    AuthoredSentence(
        kasiguranin = listOf("Mabigsək", "ang", "parəs", "kagibi."),
        english = "The wind was strong last night."
    ),
    AuthoredSentence(
        kasiguranin = listOf("Karon", "na, ", "kuman", "tayo!"),
        english = "Let's go, let's eat!"
    ),
    AuthoredSentence(
        kasiguranin = listOf("Me", "tólay", "sa", "baláy."),
        english = "There is a person in the house."
    ),
    AuthoredSentence(
        kasiguranin = listOf("Walang", "tólay", "sa", "baláy."),
        english = "There is no person in the house."
    ),
    AuthoredSentence(
        kasiguranin = listOf("Mag-uden", "ngayon."),
        english = "It is raining now."
    ),
    AuthoredSentence(
        kasiguranin = listOf("Madisalad", "ang", "bulos."),
        english = "The river is deep."
    ),
    AuthoredSentence(
        kasiguranin = listOf("Saan", "ka", "umangay?"),
        english = "Where are you going?"
    ),
    AuthoredSentence(
        kasiguranin = listOf("Namúgtong", "ang", "anák", "ng", "mángga."),
        english = "I bought a mango."
    ),
    AuthoredSentence(
        kasiguranin = listOf("Kinumán", "na", "ku'", "ng", "kanən."),
        english = "I already ate rice."
    ),
    AuthoredSentence(
        kasiguranin = listOf("Ang", "sida", "me", "ay", "manok."),
        english = "Our viand is chicken."
    )
    )

    /**
     * A sentence that uses [word], or null when none does.
     *
     * Matched on the headword appearing as one of the sentence's own words, punctuation and case
     * ignored, so *anák* matches "anák." at the end of a sentence. A lesson only earns a sentence
     * exercise when the sentence genuinely contains one of the words it is teaching; an unrelated
     * sentence would be a grammar drill wearing a vocabulary lesson's clothes.
     */
    fun sentenceUsing(word: String): AuthoredSentence? {
        val needle = normalise(word)
        if (needle.isEmpty()) return null
        return sentences.firstOrNull { sentence ->
            sentence.kasiguranin.any { normalise(it) == needle }
        }
    }

    /** Lowercased and stripped of the punctuation a word carries when it ends a sentence. */
    fun normalise(token: String): String =
        token.lowercase().trim('.', ',', '!', '?', ';', ':', '\'', '"', ' ')
}

/** One authored sentence: its words in order, and what it means. */
data class AuthoredSentence(
    /** The sentence's words, already split, in the order they belong. */
    val kasiguranin: List<String>,
    val english: String
) {
    val text: String get() = kasiguranin.joinToString(" ")
}
