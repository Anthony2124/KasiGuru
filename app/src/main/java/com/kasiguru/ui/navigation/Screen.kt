package com.kasiguru.ui.navigation

/**
 * Sealed class defining all navigation routes in KasiGuru.
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object ProfileSelection : Screen("profile_selection")
    /**
     * The learner's home: today's one next action, the day goal, reviews due, stories and the week.
     * Where every entry into the app lands - splash, onboarding and profile selection all end here.
     */
    data object Home : Screen("home")
    /** The learning path alone: the sections as a journey, with the node to tap next. */
    data object Learn : Screen("learn")
    /** Words and Stories behind one segmented toggle. [VocabularyList] and [StoryList] stay as pushed routes. */
    data object Library : Screen("library")
    data object LessonPlayer : Screen("lesson/{unitId}/{lessonIndex}") {
        /** Unit ids are category names and contain spaces and ampersands, so they must be encoded. */
        fun createRoute(unitId: String, lessonIndex: Int): String {
            val encoded = encodeRouteArg(unitId)
            return "lesson/$encoded/$lessonIndex"
        }
    }
    /** The Me tab. The route keeps its old name so notification deep links written against it still land. */
    data object Profile : Screen("profile")
    data object PublicProfile : Screen("player/{uid}") {
        fun createRoute(uid: String) = "player/${encodeRouteArg(uid)}"
    }
    data object EditProfile : Screen("edit_profile")
    /** The stories list on its own, pushed. The Library tab shows the same content under its Stories segment. */
    data object StoryList : Screen("stories")
    data object StoryReader : Screen("story/{storyId}") {
        fun createRoute(storyId: Int) = "story/$storyId"
    }
    /** The dictionary on its own, pushed. The Library tab shows the same content under its Words segment. */
    data object VocabularyList : Screen("vocabulary")
    data object VocabularyDetail : Screen("vocabulary/{wordId}") {
        fun createRoute(wordId: Int) = "vocabulary/$wordId"
    }
    data object VocabularyCategory : Screen("vocabulary/category/{category}") {
        /**
         * Category names are display strings - "Body Parts & Health", "Food & Dining" - so the
         * argument has to be encoded for the same reason [LessonPlayer.createRoute] encodes its unit
         * id. Harmless while the only caller passes a name straight from the registry; a broken route
         * the moment anything resolves a category at runtime.
         */
        fun createRoute(category: String): String {
            val encoded = encodeRouteArg(category)
            return "vocabulary/category/$encoded"
        }
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
    data object WordWheelGame : Screen("games/word_wheel/{level}") {
        fun createRoute(level: Int) = "games/word_wheel/$level"
    }
    /** Asks which category to play; each category's own levels then open in [LevelSelection]. */
    data object WordSearchCategories : Screen("games/word_search")
    /** [category] is a `Constants.Games.wordSearchLevelKey`, already a route-safe slug. */
    data object WordSearchGame : Screen("games/word_search/{category}/{level}") {
        fun createRoute(category: String, level: Int) = "games/word_search/$category/$level"
    }
    /** Every badge. Pushed from Me and from Home's goal ring; no longer a tab of its own. */
    data object Achievements : Screen("achievements")
    data object CulturalContext : Screen("cultural")
    data object FlashcardDeck : Screen("flashcards")
    data object Leaderboard : Screen("leaderboard")
    data object Notifications : Screen("notifications")
    data object Streak : Screen("streak")
    data object Appearance : Screen("appearance")
    data object Settings : Screen("settings")
    data object Account : Screen("account")
    data object About : Screen("about")
    data object Help : Screen("help")
    data object SubmitWord : Screen("submit_word")
    data object SubmitLiterature : Screen("submit_literature")
    data object ReportIssue : Screen("report_issue?category={category}&word={word}&screenContext={screenContext}") {
        fun createRoute(
            category: String? = null,
            word: String? = null,
            screenContext: String? = null
        ): String {
            val params = mutableListOf<String>()
            category?.let { params.add("category=${encodeRouteArg(it)}") }
            word?.let { params.add("word=${encodeRouteArg(it)}") }
            screenContext?.let { params.add("screenContext=${encodeRouteArg(it)}") }
            return if (params.isEmpty()) "report_issue" else "report_issue?${params.joinToString("&")}"
        }
    }

    /** Shown when an admin has banned the current signed-in account. No back navigation. */
    data object AccountSuspended : Screen("account_suspended")

    companion object {
        /**
         * The five destinations the bottom bar shows, and the only routes that may be reached by
         * switching tabs rather than by pushing.
         *
         * Declared once here because three places need to agree on it - the navigation graph's
         * bottom-bar visibility, the guided tour's navigator, and the tour's own tests - and they
         * each kept a private copy of the list until now.
         */
        /**
         * Computed on access, not stored.
         *
         * A `val` here would be evaluated while `Screen` itself is still initialising, and reading
         * `Home.route` at that moment re-enters the very class initialiser that is running - the
         * nested object's INSTANCE is still null, and the whole class fails to load with a
         * NoClassDefFoundError that names nothing useful. Deferring to a getter sidesteps it, and a
         * five-element set is not worth caching.
         *
         * Home, Learn, Practice, Library, Me - in the order the bar shows them. The dictionary, the
         * stories list and the badges used to be tabs; they are now reached through Library and Me,
         * and their own routes survive as pushed screens for deep links.
         */
        val tabRoots: Set<String>
            get() = setOf(
                Home.route,
                Learn.route,
                GameHub.route,
                Library.route,
                Profile.route
            )
    }
}

/**
 * Percent-encodes one route argument.
 *
 * [java.net.URLEncoder] writes *form* encoding, in which a space becomes `+`. Navigation decodes a
 * route argument with `Uri.decode`, which follows RFC 3986 and leaves `+` exactly as it found it, so
 * a display string round-tripped through URLEncoder alone arrives at the screen as
 * `Greetings+&+Essentials`. Every one of the twelve category names contains spaces, which meant every
 * category screen matched on a name no word in the corpus has and rendered "0 words" — the dictionary
 * looked empty from the inside while holding twelve hundred entries.
 *
 * Rewriting `+` as `%20` is what makes the two halves agree. Deliberately not `android.net.Uri.encode`,
 * which would do the same job: this file is read by a JVM unit test, and `Uri` is unimplemented there.
 */
private fun encodeRouteArg(value: String): String =
    java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
