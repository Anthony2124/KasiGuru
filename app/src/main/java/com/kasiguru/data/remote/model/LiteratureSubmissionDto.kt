package com.kasiguru.data.remote.model

import com.google.firebase.firestore.DocumentId

/**
 * A user-submitted story or poem, extending the same create-only-by-submitter,
 * admin-only-review shape as [WordSubmissionDto]. Lands in Firestore
 * `literature_submissions/{id}` with `status = "pending"`; the admin panel copies an
 * approved submission into the real `stories` collection, the same copy-on-approve
 * pattern word submissions already use rather than writing to live tables directly.
 *
 * [pagesJson] mirrors [com.kasiguru.data.local.entity.StoryPage]'s shape (a JSON array of
 * `{pageNumber, kasiguranin, tagalog, english}` objects) so an approved submission needs no
 * reshaping to become a story. Submissions are text-only - no illustrations - since Adrian
 * authors all artwork himself; art is added afterward through the existing story-authoring
 * flow if a submission is approved.
 */
data class LiteratureSubmissionDto(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val titleKasiguranin: String = "",
    val pagesJson: String = "[]",
    val contributorName: String = "Anonymous",
    val status: String = "pending", // "pending", "approved", "rejected"
    val submittedAt: Long = System.currentTimeMillis(),
    val uid: String = "" // Firebase Auth uid of the submitter ("" if anonymous sign-in hasn't completed)
)
