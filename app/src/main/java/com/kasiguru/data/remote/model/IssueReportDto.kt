package com.kasiguru.data.remote.model

import com.google.firebase.firestore.DocumentId

/**
 * A user-submitted bug report or wrong word/translation correction.
 * Stored in Firestore `issue_reports/{id}` with `status = "pending"`.
 *
 * [photoBase64] stores a downsampled, compressed JPEG/WebP base64 data string (e.g. data:image/jpeg;base64,...)
 * for immediate rendering in both client apps and the admin dashboard without requiring external storage buckets.
 */
data class IssueReportDto(
    @DocumentId
    val id: String = "",
    val category: String = "Bug / System Issue", // "Bug / System Issue", "Wrong Word / Translation", "Grammar / Literature", "Audio Issue", "Other"
    val title: String = "",
    val description: String = "",
    val targetWord: String = "",
    val targetScreen: String = "",
    val photoBase64: String = "",
    val photoUrl: String = "",
    val reporterName: String = "Anonymous",
    val reporterEmail: String = "",
    val appVersion: String = "",
    val deviceInfo: String = "",
    val status: String = "pending", // "pending", "in_review", "resolved", "dismissed"
    val adminNotes: String = "",
    val submittedAt: Long = System.currentTimeMillis(),
    val uid: String = "" // Firebase Auth UID if signed in
)
