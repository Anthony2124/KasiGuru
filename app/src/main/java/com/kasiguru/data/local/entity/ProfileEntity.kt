package com.kasiguru.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A named local profile for shared-device/family use, e.g. two siblings sharing one phone.
 *
 * This is the identity/roster layer only - step one of the backlog's "profile creation with
 * avatar selection... data model for accounts with multiple profiles." [UserProgressEntity]
 * stays a single row (`id = 1`) in this pass; every profile currently reads and writes that same
 * progress. Giving each profile fully separate wordsLearned/streak/achievements/lesson-progress
 * state is real follow-up work (a composite-key migration touching UserProgressDao and every
 * repository that reads "the" progress row, plus a Firestore-side redesign since cloud sync is
 * keyed by uid, not by profile) - attempting that in the same pass as this roster risks the
 * exact kind of subtle data-loss bug a schema change to real users' stored progress can't afford
 * to risk without its own dedicated design pass.
 *
 * What this DOES deliver for real: a profile-selection screen after login, and named profiles
 * with their own avatar and display name, addressing the "which family member is this" part of
 * the shared-device use case even before progress itself is split.
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    /** [com.kasiguru.ui.components.CasiguranResident] name, e.g. "STUDENT" - see ResidentIcons. */
    val residentName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = false
)
