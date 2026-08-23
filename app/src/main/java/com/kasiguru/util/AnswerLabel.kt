package com.kasiguru.util

/**
 * Reads an answer button's text back into the words it was built from.
 *
 * A gloss option is printed as "tagalog · english" so both audiences can read it, which means the
 * string on the button matches no single database column. Anything that needs to look up what the
 * learner tapped — remediation after a wrong answer, above all — has to undo that formatting first.
 */
object AnswerLabel {

    private const val GLOSS_SEPARATOR = '·'

    /**
     * The label itself first, then its parts.
     *
     * Order matters: a headword is matched whole before it is split, so a word that legitimately
     * contains the separator is never broken up in preference to itself.
     */
    fun candidates(label: String): List<String> {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return emptyList()

        val parts = trimmed.split(GLOSS_SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return (listOf(trimmed) + parts).distinct()
    }
}
