package com.kasiguru.domain.gamification

import com.kasiguru.data.local.entity.AchievementEntity
import com.kasiguru.util.pluralize

enum class BadgeTier(val label: String, val bonus: Int) {
    BEGINNER("Beginner", 20), LEARNER("Learner", 40), ACHIEVER("Achiever", 80),
    EXPERT("Expert", 120), MASTER("Master", 160), LEGEND("Legend", 200)
}

/** @param unit what the metric counts, singular ("day"); [units] is its plural. */
data class BadgeFamily(val id: String, val name: String, val metric: String, val unit: String,
    val thresholds: List<Int>, val section: String, val action: String, val bonus: Boolean = true,
    val units: String = unit + "s") {

    /** A count in this family's unit: "1 day", "30 days". The level family reads as a rank: "Level 6". */
    fun amount(n: Int): String = if (metric == "level") "Level $n" else pluralize(n, unit, units)

    /** How far toward [target]: "0 / 1 day", "12 / 30 days", "Level 3 of 6". */
    fun progress(value: Int, target: Int): String =
        if (metric == "level") "Level $value of $target" else "$value / ${amount(target)}"
}

/** Stable identities; thresholds count distinct evidence, never screen visits. */
object BadgeCatalog {
    const val PREFIX = "badge:"
    val families = listOf(
        BadgeFamily("word_explorer", "Word Explorer", "verifiedWords", "word", listOf(1,10,50,150,400,800), "Learning", "Review words"),
        BadgeFamily("lesson_pathfinder", "Lesson Pathfinder", "distinctLessons", "lesson", listOf(1,5,10,30,75,150), "Learning", "Continue learning"),
        BadgeFamily("review_keeper", "Review Keeper", "scheduledReviews", "review", listOf(10,20,100,300,1000,3000), "Learning", "Review words"),
        BadgeFamily("consistent_learner", "Consistent Learner", "streak", "day", listOf(1,3,7,30,90,180), "Practice", "View today's tasks"),
        BadgeFamily("game_adventurer", "Game Adventurer", "distinctGameLevels", "level", listOf(1,5,10,50,150,400), "Games", "Play a game"),
        BadgeFamily("precision_player", "Precision Player", "perfectLevels", "perfect level", listOf(1,3,5,20,75,150), "Games", "Play a game"),
        BadgeFamily("mode_explorer", "Mode Explorer", "gameModesPlayed", "mode", listOf(1,2,3,4,6,8), "Games", "Explore games"),
        BadgeFamily("story_reader", "Story Reader", "storiesCompleted", "story", listOf(1,2,3,4,7,10), "Learning", "Read a story", units = "stories"),
        BadgeFamily("category_scholar", "Category Scholar", "verifiedCategories", "category", listOf(1,2,3,6,9,12), "Learning", "Explore vocabulary", units = "categories"),
        BadgeFamily("community_contributor", "Community Contributor", "submissionsApproved", "approval", listOf(1,3,5,10,25,50), "Community", "Contribute a word"),
        BadgeFamily("journey_rank", "Journey Rank", "level", "account level", listOf(2,3,6,10,20,30), "Practice", "Continue learning", bonus = false)
    )
    fun familyFor(id: String): BadgeFamily? = tierFor(id)?.let { tier ->
        families.firstOrNull { id == "$PREFIX${it.id}:${tier.ordinal+1}" }
    }
    fun tierFor(id: String): BadgeTier? = id.substringAfterLast(':').toIntOrNull()?.let { BadgeTier.entries.getOrNull(it - 1) }
    fun rows(): List<AchievementEntity> = families.flatMap { family -> BadgeTier.entries.mapIndexed { i, tier ->
        AchievementEntity(id = "$PREFIX${family.id}:${i+1}", name = family.name,
            description = (if (family.metric == "level") "Reach " else "Earn ") + family.amount(family.thresholds[i]), iconEmoji = "", category = family.section,
            requiredValue = family.thresholds[i], metricType = family.metric, tier = tier.name.lowercase(),
            xpReward = if (family.bonus) tier.bonus else 0)
    } }
}
