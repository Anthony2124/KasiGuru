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
        fun displayName(userName: String): String = userName.trim().take(40).takeUnless { it.isBlank() || '@' in it } ?: "Learner"
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
