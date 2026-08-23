package com.kasiguru.util.gamification

import com.kasiguru.util.Constants

data class LevelInfo(
    val level: Int,
    val title: String,
    val minXp: Int,
    val maxXp: Int,
    val iconEmoji: String,
    val unlockedGameTypes: List<String>
)

object GamificationEngine {

    // Titles, icons, and game unlocks for the five display ranks (1-5).
    // XP boundaries are derived from Constants.LEVEL_THRESHOLDS (the single
    // source of truth) so the level shown in the UI always agrees with the
    // stored user level and achievements. Ranks cap at 5 ("Kasiguranin Legend"),
    // even though the stored level can reach 10.
    private val LEVEL_TITLES = listOf(
        "Novice Explorer",
        "Vocab Apprentice",
        "Linguistic Scholar",
        "Grammar Specialist",
        "Kasiguranin Legend"
    )

    private val LEVEL_ICONS = listOf("🌱", "📚", "🎧", "⚡", "👑")

    // Constants.Games.RECALL is the key Word Recall inherited from the Audio Quiz it replaced, so
    // the tables below keep gating the same slot they always did.
    private val LEVEL_GAME_UNLOCKS = listOf(
        listOf("word_match"),
        listOf("word_match", "reverse_match", "fill_blank"),
        listOf("word_match", "reverse_match", "fill_blank", Constants.Games.RECALL),
        listOf("word_match", "reverse_match", "fill_blank", Constants.Games.RECALL, "aspect_builder"),
        listOf(
            "word_match", "reverse_match", "fill_blank", Constants.Games.RECALL,
            "aspect_builder", "sentence_order"
        )
    )

    val LEVELS: List<LevelInfo> =
        Constants.LEVEL_THRESHOLDS.take(5).mapIndexed { i, minXp ->
            LevelInfo(
                level = i + 1,
                title = LEVEL_TITLES[i],
                minXp = minXp,
                maxXp = if (i == 4) Int.MAX_VALUE else Constants.LEVEL_THRESHOLDS[i + 1] - 1,
                iconEmoji = LEVEL_ICONS[i],
                unlockedGameTypes = LEVEL_GAME_UNLOCKS[i]
            )
        }

    fun getLevelInfo(totalXp: Int): LevelInfo {
        return LEVELS.lastOrNull { totalXp >= it.minXp } ?: LEVELS.first()
    }

    fun getNextLevelInfo(currentLevel: Int): LevelInfo? {
        return LEVELS.firstOrNull { it.level > currentLevel }
    }

    fun getXpProgressInLevel(totalXp: Int): Float {
        val current = getLevelInfo(totalXp)
        val next = getNextLevelInfo(current.level) ?: return 1.0f
        val range = next.minXp - current.minXp
        val progress = totalXp - current.minXp
        return (progress.toFloat() / range.toFloat()).coerceIn(0.0f, 1.0f)
    }

    fun isGameUnlocked(gameType: String, userLevel: Int, accuracyRate: Float = 1.0f): Boolean {
        val minReq = when (gameType) {
            "word_match" -> 1
            "reverse_match" -> 2
            "fill_blank" -> 2
            Constants.Games.RECALL -> 3
            "aspect_builder" -> 4
            "sentence_order" -> 5
            else -> 1
        }
        if (minReq <= 1) return userLevel >= minReq
        return userLevel >= minReq && accuracyRate >= 0.70f
    }

    fun getRequiredLevelForGame(gameType: String): Int {
        return when (gameType) {
            "word_match" -> 1
            "reverse_match" -> 2
            "fill_blank" -> 2
            Constants.Games.RECALL -> 3
            "aspect_builder" -> 4
            "sentence_order" -> 5
            else -> 1
        }
    }

}
