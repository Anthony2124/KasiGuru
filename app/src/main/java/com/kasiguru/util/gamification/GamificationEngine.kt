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

    val LEVELS: List<LevelInfo> =
        Constants.LEVEL_THRESHOLDS.mapIndexed { i, minXp ->
            LevelInfo(
                level = i + 1,
                title = "Level ${i + 1}",
                minXp = minXp,
                maxXp = if (i == Constants.LEVEL_THRESHOLDS.lastIndex) Int.MAX_VALUE else Constants.LEVEL_THRESHOLDS[i + 1] - 1,
                iconEmoji = "",
                unlockedGameTypes = emptyList()
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

}
