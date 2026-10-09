package com.kasiguru.data.remote.model

/**
 * An example sentence a learner offers for a word with none, in `sentence_submissions`. The word is
 * named by its sense ([kasiguranin] + [english]), since a Room id means nothing to Firestore. A
 * verifier approves it in the admin portal, which writes it onto the vocabulary document; until then
 * it is visible to no one else. Fields match firestore.rules' isValidSentenceSubmission().
 */
data class SentenceSubmissionDto(
    val kasiguranin: String,
    val english: String,
    val sentence: String,
    val translation: String,
    val contributorName: String
)
