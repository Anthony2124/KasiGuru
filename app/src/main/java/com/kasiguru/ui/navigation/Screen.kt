package com.kasiguru.ui.navigation

/**
 * Sealed class defining all navigation routes in KasiGuru.
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object ProfileSelection : Screen("profile_selection")
    /** The learner's home: today's plan. Replaces the old Home dashboard. */
    data object Learn : Screen("learn")
    data object LessonPlayer : Screen("lesson/{unitId}/{lessonIndex}") {
        /** Unit ids are category names and contain spaces and ampersands, so they must be encoded. */
        fun createRoute(unitId: String, lessonIndex: Int): String {
            val encoded = java.net.URLEncoder.encode(unitId, "UTF-8")
            return "lesson/$encoded/$lessonIndex"
        }
    }
    data object Profile : Screen("profile")
    data object EditProfile : Screen("edit_profile")
    data object StoryList : Screen("stories")
    data object StoryReader : Screen("story/{storyId}") {
        fun createRoute(storyId: Int) = "story/$storyId"
    }
    data object VocabularyList : Screen("vocabulary")
    data object VocabularyDetail : Screen("vocabulary/{wordId}") {
        fun createRoute(wordId: Int) = "vocabulary/$wordId"
    }
    data object VocabularyCategory : Screen("vocabulary/category/{category}") {
        fun createRoute(category: String) = "vocabulary/category/$category"
    }
    data object GameHub : Screen("games")
    data object LevelSelection : Screen("games/levels/{gameType}") {
        fun createRoute(gameType: String) = "games/levels/$gameType"
    }
    data object WordMatchGame : Screen("games/word_match/{level}") {
        fun createRoute(level: Int) = "games/word_match/$level"
    }
    data object ReverseMatchGame : Screen("games/reverse_match/{level}") {
        fun createRoute(level: Int) = "games/reverse_match/$level"
    }
    data object FillBlankGame : Screen("games/fill_blank/{level}") {
        fun createRoute(level: Int) = "games/fill_blank/$level"
    }
    data object RecallGame : Screen("games/recall/{level}") {
        fun createRoute(level: Int) = "games/recall/$level"
    }
    data object AspectBuilderGame : Screen("games/aspect_builder/{level}") {
        fun createRoute(level: Int) = "games/aspect_builder/$level"
    }
    data object SentenceOrderGame : Screen("games/sentence_order/{level}") {
        fun createRoute(level: Int) = "games/sentence_order/$level"
    }
    data object Achievements : Screen("achievements")
    data object CulturalContext : Screen("cultural")
    data object FlashcardDeck : Screen("flashcards")
    data object Leaderboard : Screen("leaderboard")
    data object Notifications : Screen("notifications")
    data object Settings : Screen("settings")
    data object Account : Screen("account")
    data object About : Screen("about")
    data object SubmitWord : Screen("submit_word")
    data object SubmitLiterature : Screen("submit_literature")
    data object ReportIssue : Screen("report_issue?category={category}&word={word}&screenContext={screenContext}") {
        fun createRoute(
            category: String? = null,
            word: String? = null,
            screenContext: String? = null
        ): String {
            val params = mutableListOf<String>()
            category?.let { params.add("category=${java.net.URLEncoder.encode(it, "UTF-8")}") }
            word?.let { params.add("word=${java.net.URLEncoder.encode(it, "UTF-8")}") }
            screenContext?.let { params.add("screenContext=${java.net.URLEncoder.encode(it, "UTF-8")}") }
            return if (params.isEmpty()) "report_issue" else "report_issue?${params.joinToString("&")}"
        }
    }
}
