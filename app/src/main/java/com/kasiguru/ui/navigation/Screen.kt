package com.kasiguru.ui.navigation

/**
 * Sealed class defining all navigation routes in KasiGuru.
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
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
    data object WordMatchGame : Screen("games/word_match")
    data object FillBlankGame : Screen("games/fill_blank")
    data object AudioQuizGame : Screen("games/audio_quiz")
    data object AspectBuilderGame : Screen("games/aspect_builder")
    data object SentenceOrderGame : Screen("games/sentence_order")
    data object Achievements : Screen("achievements")
    data object Settings : Screen("settings")
    data object CulturalContext : Screen("cultural")
    data object FlashcardDeck : Screen("flashcards")
    data object About : Screen("about")
}
