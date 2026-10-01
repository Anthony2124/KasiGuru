package com.kasiguru.domain.gamification

import com.kasiguru.data.local.entity.AchievementEntity

/** Archived display metadata, used only to restore badges already earned on an older install. */
object LegacyBadgeCatalog {
    private val definitions = mapOf(
        "level_1" to ("Novice Explorer" to "Began your Kasiguranin learning journey"),
        "level_2" to ("Vocab Apprentice" to "Reached Level 2 & unlocked Fill in the Blank"),
        "level_3" to ("Linguistic Scholar" to "Reached Level 3 & unlocked Audio Listening Quiz"),
        "level_4" to ("Grammar Specialist" to "Reached Level 4 & unlocked Verb Aspect Builder"),
        "level_5" to ("Kasiguranin Legend" to "Reached Level 5 & unlocked Sentence Construction"),
        "first_word" to ("Unáng Salitâ" to "Learn your first Kasiguranin word"),
        "ten_words" to ("Sampûng Salitâ" to "Learn 10 Kasiguranin words"),
        "fifty_words" to ("Limampûng Salitâ" to "Learn 50 Kasiguranin words"),
        "first_story" to ("Mambábasa" to "Complete your first story"),
        "first_game" to ("Mánlalaro" to "Play your first mini-game"),
        "perfect_game" to ("Perpekto!" to "Get a perfect score in any mini-game"),
        "three_day_streak" to ("Tatlong Aldaw" to "Maintain a 3-day learning streak"),
        "seven_day_streak" to ("Isáng Linggo" to "Maintain a 7-day learning streak"),
        "level_five" to ("Sumusulong" to "Reach Level 5"),
        "level_ten" to ("Mæstro" to "Reach Level 10 — Master of Kasiguranin!"),
        "all_stories" to ("Tagapagsalaysay" to "Complete 3 stories"),
        "first_contribution" to ("First Contribution" to "Submit your first word, story, or poem"),
        "trusted_voice" to ("Trusted Voice" to "Have 5 submissions approved into the dictionary or Stories"),
        "corpus_builder" to ("Corpus Builder" to "Have 25 submissions approved into the dictionary or Stories"),
        "moon_cycle" to ("Moon Cycle" to "Maintain a 30-day learning streak"),
        "centurion" to ("Centurion" to "Maintain a 100-day learning streak"),
        "category_master" to ("Category Master" to "Learn every word in one dictionary category"),
        "perfect_six" to ("Perfect Six" to "Score perfectly in all six game modes in one sitting"),
        "top_of_the_week" to ("Top of the Week" to "Reach the top 10 of the weekly leaderboard"),
        "six_for_six" to ("Six for Six" to "Play every one of the six mini-game modes at least once")
    )

    fun definition(id: String): AchievementEntity {
        val metadata = definitions[id]
        return AchievementEntity(id = id, name = metadata?.first ?: id,
            description = metadata?.second ?: "Previously earned achievement",
            iconEmoji = "", category = "Legacy", xpReward = 0)
    }
}
