package com.kasiguru.util

/**
 * App-wide constants for KasiGuru.
 */
object Constants {

    // Gamification
    const val XP_PER_WORD_LEARNED = 100
    const val XP_PER_STORY_PAGE = 15
    const val XP_PER_STORY_COMPLETE = 50
    const val XP_PER_GAME_CORRECT = 20
    const val XP_BONUS_PERFECT_GAME = 100
    const val XP_STREAK_BONUS = 25
    const val STREAK_RESET_HOURS = 24

    // Levels — XP thresholds
    val LEVEL_THRESHOLDS = listOf(
        0,      // Level 1
        100,    // Level 2
        300,    // Level 3
        600,    // Level 4
        1000,   // Level 5
        1500,   // Level 6
        2200,   // Level 7
        3000,   // Level 8
        4000,   // Level 9
        5000    // Level 10
    )

    // Level titles in Kasiguranin
    val LEVEL_TITLES = listOf(
        "Baguhan",       // Beginner
        "Nag-aaral",     // Learner
        "Nagsisimula",   // Starter
        "Lumalago",      // Growing
        "Sumusulong",    // Advancing
        "Mahusay",       // Skilled
        "Dalubhasa",     // Expert
        "Pantas",        // Wise
        "Guro",          // Teacher
        "Mæstro"         // Master
    )

    // Categories for vocabulary
    object VocabCategories {
        const val GREETINGS = "Greetings"
        const val FAMILY = "Family"
        const val NATURE = "Nature"
        const val FOOD = "Food"
        const val NUMBERS = "Numbers"
        const val BODY = "Body"
        const val ANIMALS = "Animals"
        const val DAILY = "Daily Activities"

        // Single source of truth for the category dropdowns (Submit Word screen).
        val ALL = listOf(
            "Greetings & Essentials",
            "Body Parts & Health",
            "Animals & Wildlife",
            "Food & Dining",
            "Numbers & Time",
            "Weather & Climate",
            "Nature & Environment",
            "Family & People",
            "Emotions & Feelings",
            "Colors & Shapes",
            "Occupations & Tools"
        )
    }

    // Achievement IDs
    object Achievements {
        const val FIRST_WORD = "first_word"
        const val TEN_WORDS = "ten_words"
        const val FIFTY_WORDS = "fifty_words"
        const val FIRST_STORY = "first_story"
        const val ALL_STORIES = "all_stories"
        const val FIRST_GAME = "first_game"
        const val PERFECT_GAME = "perfect_game"
        const val THREE_DAY_STREAK = "three_day_streak"
        const val SEVEN_DAY_STREAK = "seven_day_streak"
        const val LEVEL_FIVE = "level_five"
        const val LEVEL_TEN = "level_ten"
    }

    // Games
    object Games {
        const val WORD_MATCH = "word_match"
        const val REVERSE_MATCH = "reverse_match"
        const val ASPECT_BUILDER = "aspect_builder"
        const val SENTENCE_ORDER = "sentence_order"
    }

    // Mini-Game Unlock Requirements (Total Stars)
    object GameUnlockStars {
        const val WORD_MATCH = 0
        const val FILL_BLANK = 45
        const val AUDIO_QUIZ = 90
        const val ASPECT_BUILDER = 135
        const val SENTENCE_ORDER = 180
        const val REVERSE_MATCH = 225
    }

    // DataStore keys
    object Prefs {
        const val ONBOARDING_COMPLETE = "onboarding_complete"
        const val USER_NAME = "user_name"
        const val DARK_MODE = "dark_mode"
        const val SOUND_ENABLED = "sound_enabled"
    }
}
