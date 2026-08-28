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

        /**
         * The category dropdowns (Submit Word screen).
         *
         * Must stay identical to the categories in
         * [com.kasiguru.ui.theme.CategoryRegistry], which is what the Dictionary screen actually
         * renders as cards. This list was missing "House & Daily Life" -- the second-largest
         * category in the corpus -- so a contributor could not file a word under it, and a word
         * that landed outside the registry got no category card and was reachable only by search.
         */
        val ALL = listOf(
            "Greetings & Essentials",
            "Body Parts & Health",
            "Animals & Wildlife",
            "Food & Dining",
            "Numbers & Time",
            "Weather & Climate",
            "Nature & Environment",
            "House & Daily Life",
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

        // New achievements (badge redesign): submission-based, deeper streak/mastery, and
        // social/leaderboard-based, matching the categories chosen for the roadmap.
        const val FIRST_CONTRIBUTION = "first_contribution"
        const val TRUSTED_VOICE = "trusted_voice"
        const val CORPUS_BUILDER = "corpus_builder"
        const val MOON_CYCLE = "moon_cycle"
        const val CENTURION = "centurion"
        const val CATEGORY_MASTER = "category_master"
        const val PERFECT_SIX = "perfect_six"
        const val TOP_OF_THE_WEEK = "top_of_the_week"
        const val SIX_FOR_SIX = "six_for_six"
    }

    // Games
    object Games {
        const val WORD_MATCH = "word_match"
        const val REVERSE_MATCH = "reverse_match"
        const val ASPECT_BUILDER = "aspect_builder"
        const val SENTENCE_ORDER = "sentence_order"

        /**
         * The typed-recall game.
         *
         * Its stored key is still "audio_quiz" because Recall took that slot over rather than being
         * added beside it. Every level row, star total and high score already keyed to "audio_quiz"
         * -- locally and inside the synced `gameLevels` document -- keeps counting. Renaming the key
         * would strand that progress behind a Room migration and a cloud merge shim, for nothing the
         * learner can see.
         */
        const val RECALL = "audio_quiz"
    }

    // Mini-Game Unlock Requirements (Total Stars)
    object GameUnlockStars {
        const val WORD_MATCH = 0
        const val FILL_BLANK = 45
        const val RECALL = 90
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
