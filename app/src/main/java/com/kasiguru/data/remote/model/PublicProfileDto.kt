package com.kasiguru.data.remote.model

import com.kasiguru.domain.gamification.BadgeCatalog
import com.kasiguru.domain.gamification.ProfileBackgroundCatalog

/** Deliberately cannot carry personal fields from Edit profile. */
data class PublicProfileDto(
    val displayName: String = "Learner",
    val profileIconId: Int = 1,
    val profileBackgroundId: String = "forest",
    val level: Int = 1,
    val totalXp: Int = 0,
    val currentStreak: Int = 0,
    val wordsLearned: Int = 0,
    val lessonsCompleted: Int = 0,
    val weeklyXp: Int = 0,
    val weekId: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val badgeIds: List<String> = emptyList(),
    val sections: Map<String, Int> = emptyMap(),
    val sectionTotals: Map<String, Int> = emptyMap(),
    val masteredSections: List<String> = emptyList(),
    val unlockedSections: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "displayName" to displayName, "profileIconId" to profileIconId,
        "profileBackgroundId" to profileBackgroundId, "level" to level, "totalXp" to totalXp,
        "currentStreak" to currentStreak, "wordsLearned" to wordsLearned, "lessonsCompleted" to lessonsCompleted,
        "weeklyXp" to weeklyXp, "weekId" to weekId, "createdAt" to createdAt, "updatedAt" to updatedAt,
        "badgeIds" to badgeIds, "sections" to sections, "sectionTotals" to sectionTotals,
        "masteredSections" to masteredSections, "unlockedSections" to unlockedSections
    )
    companion object {
        /** The nickname every progress row starts with, before onboarding asks for one. */
        const val PLACEHOLDER_NAME = "Learner"

        fun displayName(userName: String): String = displayName(userName, fullName = "")

        /**
         * The name the leaderboard and public profile show: the learner's nickname, or their full
         * name when no nickname was ever set. Never an email.
         *
         * Someone who signs in from the first onboarding screen skips the step that asks for a
         * nickname, so theirs stays [PLACEHOLDER_NAME]. Publishing that alone ranked them as
         * "Learner" with no way out: Edit profile changes the full name, not the nickname, and the
         * account screen already tells them the full name becomes their leaderboard name.
         */
        fun displayName(userName: String, fullName: String): String =
            listOf(userName, fullName)
                .map { it.trim().take(40) }
                .firstOrNull { it.isNotBlank() && '@' !in it && it != PLACEHOLDER_NAME }
                ?: PLACEHOLDER_NAME
        fun fromMap(data: Map<String, Any>): PublicProfileDto {
            fun count(key: String) = (data[key] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0
            fun strings(key: String) = (data[key] as? List<*>)?.filterIsInstance<String>().orEmpty()
            fun counts(key: String) = (data[key] as? Map<*, *>)?.mapNotNull { (id, value) ->
                if (id is String && value is Number) id to value.toInt().coerceAtLeast(0) else null
            }?.toMap().orEmpty()
            val allowedBadges = BadgeCatalog.rows().map { it.id }.toSet()
            val sectionIds = com.kasiguru.domain.lesson.LearningTree.sections.map { it.id }.toSet()
            return PublicProfileDto(
                displayName = displayName(data["displayName"] as? String ?: ""),
                profileIconId = count("profileIconId"),
                profileBackgroundId = ProfileBackgroundCatalog.normalize(data["profileBackgroundId"] as? String ?: ""),
                level = count("level").coerceIn(1, 30), totalXp = count("totalXp"),
                currentStreak = count("currentStreak"), wordsLearned = count("wordsLearned"), lessonsCompleted = count("lessonsCompleted"),
                weeklyXp = count("weeklyXp"), weekId = data["weekId"] as? String ?: "",
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0,
                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0,
                badgeIds = strings("badgeIds").filter { it in allowedBadges }.distinct(),
                sections = counts("sections").filterKeys { it in sectionIds },
                sectionTotals = counts("sectionTotals").filterKeys { it in sectionIds },
                masteredSections = strings("masteredSections").filter { it in sectionIds },
                unlockedSections = strings("unlockedSections").filter { it in sectionIds }
            )
        }
    }
}
