package com.kasiguru.data.remote.model

import com.google.firebase.firestore.DocumentId

data class WordSubmissionDto(
    @DocumentId
    val id: String = "",
    val kasiguranin: String = "",
    val tagalog: String = "",
    val english: String = "",
    val rootForm: String = "",
    val category: String = "Greetings & Essentials",
    val ipaNotation: String = "",
    val exampleSentence: String = "",
    val contributorName: String = "Anonymous",
    val status: String = "pending", // "pending", "approved", "rejected"
    val submittedAt: Long = System.currentTimeMillis()
)
