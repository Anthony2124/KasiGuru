package com.kasiguru.util.gamification

data class LevelInfo(
    val level: Int,
    val title: String,
    val minXp: Int,
    val maxXp: Int,
    val iconEmoji: String,
    val unlockedGameTypes: List<String>
)

data class BadgeInfo(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val category: String,
    val isUnlocked: Boolean,
    val unlockedLevelReq: Int = 1
)

object GamificationEngine {

    val LEVELS = listOf(
        LevelInfo(
            level = 1,
            title = "Novice Explorer",
            minXp = 0,
            maxXp = 99,
            iconEmoji = "🌱",
            unlockedGameTypes = listOf("word_match")
        ),
        LevelInfo(
            level = 2,
            title = "Vocab Apprentice",
            minXp = 100,
            maxXp = 249,
            iconEmoji = "📚",
            unlockedGameTypes = listOf("word_match", "fill_blank")
        ),
        LevelInfo(
            level = 3,
            title = "Linguistic Scholar",
            minXp = 250,
            maxXp = 499,
            iconEmoji = "🎧",
            unlockedGameTypes = listOf("word_match", "fill_blank", "audio_quiz")
        ),
        LevelInfo(
            level = 4,
            title = "Grammar Specialist",
            minXp = 500,
            maxXp = 999,
            iconEmoji = "⚡",
            unlockedGameTypes = listOf("word_match", "fill_blank", "audio_quiz", "aspect_builder")
        ),
        LevelInfo(
            level = 5,
            title = "Kasiguranin Legend",
            minXp = 1000,
            maxXp = Int.MAX_VALUE,
            iconEmoji = "👑",
            unlockedGameTypes = listOf("word_match", "fill_blank", "audio_quiz", "aspect_builder", "sentence_order")
        )
    )

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

    fun isGameUnlocked(gameType: String, userLevel: Int): Boolean {
        val minReq = when (gameType) {
            "word_match" -> 1
            "fill_blank" -> 2
            "audio_quiz" -> 3
            "aspect_builder" -> 4
            "sentence_order" -> 5
            else -> 1
        }
        return userLevel >= minReq
    }

    fun getRequiredLevelForGame(gameType: String): Int {
        return when (gameType) {
            "word_match" -> 1
            "fill_blank" -> 2
            "audio_quiz" -> 3
            "aspect_builder" -> 4
            "sentence_order" -> 5
            else -> 1
        }
    }

    fun getAllBadges(userLevel: Int, wordsLearned: Int, streak: Int): List<BadgeInfo> {
        return listOf(
            BadgeInfo(
                id = "level_1",
                title = "Novice Explorer",
                description = "Began your Kasiguranin learning journey",
                iconEmoji = "🌱",
                category = "Level",
                isUnlocked = userLevel >= 1,
                unlockedLevelReq = 1
            ),
            BadgeInfo(
                id = "level_2",
                title = "Vocab Apprentice",
                description = "Reached Level 2 & unlocked Fill in the Blank",
                iconEmoji = "📚",
                category = "Level",
                isUnlocked = userLevel >= 2,
                unlockedLevelReq = 2
            ),
            BadgeInfo(
                id = "level_3",
                title = "Linguistic Scholar",
                description = "Reached Level 3 & unlocked Audio Listening Quiz",
                iconEmoji = "🎧",
                category = "Level",
                isUnlocked = userLevel >= 3,
                unlockedLevelReq = 3
            ),
            BadgeInfo(
                id = "level_4",
                title = "Grammar Specialist",
                description = "Reached Level 4 & unlocked Verb Aspect Builder",
                iconEmoji = "⚡",
                category = "Level",
                isUnlocked = userLevel >= 4,
                unlockedLevelReq = 4
            ),
            BadgeInfo(
                id = "level_5",
                title = "Kasiguranin Legend",
                description = "Reached Level 5 & unlocked Sentence Construction",
                iconEmoji = "👑",
                category = "Level",
                isUnlocked = userLevel >= 5,
                unlockedLevelReq = 5
            ),
            BadgeInfo(
                id = "words_10",
                title = "First 10 Words",
                description = "Mastered 10 Kasiguranin dictionary words",
                iconEmoji = "⭐",
                category = "Words",
                isUnlocked = wordsLearned >= 10
            ),
            BadgeInfo(
                id = "words_50",
                title = "Word Collector",
                description = "Mastered 50 Kasiguranin dictionary words",
                iconEmoji = "🏆",
                category = "Words",
                isUnlocked = wordsLearned >= 50
            ),
            BadgeInfo(
                id = "streak_3",
                title = "3-Day Streak",
                description = "Maintained a 3-day daily learning streak",
                iconEmoji = "🔥",
                category = "Streak",
                isUnlocked = streak >= 3
            ),
            BadgeInfo(
                id = "streak_7",
                title = "7-Day Warrior",
                description = "Maintained a 7-day daily learning streak",
                iconEmoji = "💪",
                category = "Streak",
                isUnlocked = streak >= 7
            )
        )
    }
}
