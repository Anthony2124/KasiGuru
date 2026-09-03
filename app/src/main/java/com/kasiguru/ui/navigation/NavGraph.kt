package com.kasiguru.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.Ground
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kasiguru.ui.components.KasiGuruBottomBar
import com.kasiguru.ui.components.LevelUpDialog
import com.kasiguru.ui.components.StreakCelebrationDialog
import com.kasiguru.ui.screens.about.AboutScreen
import com.kasiguru.ui.screens.help.HowToUseScreen
import com.kasiguru.ui.tour.LocalTourAnchors
import com.kasiguru.ui.tour.SpotlightOverlay
import com.kasiguru.ui.tour.TourAnchorRegistry
import com.kasiguru.ui.tour.TourViewModel
import com.kasiguru.ui.tour.TourChapterId
import com.kasiguru.ui.tour.TourTarget
import com.kasiguru.ui.screens.achievements.AchievementsScreen
import com.kasiguru.ui.screens.auth.AccountScreen
import com.kasiguru.ui.screens.auth.SplashViewModel
import com.kasiguru.ui.screens.cultural.CulturalScreen
import com.kasiguru.ui.screens.flashcards.FlashcardDeckScreen
import com.kasiguru.ui.screens.leaderboard.LeaderboardScreen
import com.kasiguru.ui.screens.notifications.NotificationInboxScreen
import com.kasiguru.ui.screens.games.*
import com.kasiguru.ui.screens.learn.LearnScreen
import com.kasiguru.ui.screens.lesson.LessonPlayerScreen
import com.kasiguru.ui.screens.onboarding.OnboardingScreen
import com.kasiguru.ui.screens.contribute.SubmitLiteratureScreen
import com.kasiguru.ui.screens.contribute.SubmitWordScreen
import com.kasiguru.ui.screens.profile.EditProfileScreen
import com.kasiguru.ui.screens.profile.ProfileScreen
import com.kasiguru.ui.screens.report.ReportIssueScreen
import com.kasiguru.ui.screens.settings.SettingsScreen
import com.kasiguru.ui.screens.stories.StoryListScreen
import com.kasiguru.ui.screens.stories.StoryReaderScreen
import com.kasiguru.ui.screens.vocabulary.CategoryDetailScreen
import com.kasiguru.ui.screens.vocabulary.VocabularyDetailScreen
import com.kasiguru.ui.screens.vocabulary.VocabularyScreen
import com.kasiguru.util.Constants

@Composable
fun KasiGuruNavGraph(initialDeepLink: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in Screen.tabRoots

    val levelUpViewModel: LevelUpViewModel = hiltViewModel()
    val pendingLevelUp by levelUpViewModel.pendingLevelUp.collectAsState()
    pendingLevelUp?.let { levelInfo ->
        LevelUpDialog(newLevelInfo = levelInfo, onDismiss = levelUpViewModel::dismiss)
    }

    val streakCelebrationViewModel: StreakCelebrationViewModel = hiltViewModel()
    val pendingStreakActivation by streakCelebrationViewModel.pendingStreakActivation.collectAsState()
    pendingStreakActivation?.let { streakDays ->
        StreakCelebrationDialog(streakDays = streakDays, onDismiss = streakCelebrationViewModel::dismiss)
    }

    // Moving between the five tab roots. Hoisted out of the bottom bar's own call site because the
    // guided tour drives the same navigation, and it must do it with exactly these options: a bare
    // navigate() would leave Learn→Practice→Words→Progress→Profile on the back stack, so the first
    // Back after a tour would walk the learner backwards through five screens they never chose.
    val switchTab: (String) -> Unit = { route ->
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(Screen.Learn.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val tourViewModel: TourViewModel = hiltViewModel()
    val activeTour by tourViewModel.active.collectAsState()
    val tourAnchors = remember { TourAnchorRegistry() }

    // Chapters visit pushed screens - Settings, the dictionary, Submit Word - where showBottomBar is
    // false, so the tour cannot be gated on it. What must stay excluded is the pre-app routing:
    // running while Splash is still resolving its start destination puts two navigations in flight at
    // once and empties the back stack, which shows up as a blank Ground screen rather than an error.
    val tourAllowed = currentRoute != null && currentRoute !in setOf(
        Screen.Splash.route,
        Screen.Onboarding.route,
        Screen.ProfileSelection.route
    )
    val activeStop = activeTour?.takeIf { tourAllowed }?.current
    SideEffect { tourAnchors.active = activeStop != null }
    SideEffect { tourAnchors.requestReveal(activeStop?.stop?.anchor) }

    /**
     * Moves to wherever a stop lives. Tab roots switch; anything else is pushed.
     *
     * The pushed case is a bare navigate rather than a popUpTo: a chapter is a linear walk, so the
     * stack it builds is the one the learner would have built by hand, and the tour's own Back has to
     * be able to walk back down it.
     */
    val tourNavigate: (String) -> Unit = { route ->
        when {
            route == currentRoute -> Unit
            route in Screen.tabRoots -> switchTab(route)
            else -> navController.navigate(route) { launchSingleTop = true }
        }
    }

    // Each stop names the destination it describes, so the caption is always talking about something
    // the learner can see behind the dim. Back across a stop boundary comes through here too.
    LaunchedEffect(activeTour?.chapter?.id, activeTour?.index, tourAllowed) {
        if (tourAllowed) activeStop?.let { tourNavigate(it.route) }
    }

    // A crash or a low-memory kill never delivers this, which is what the view model's debounced
    // checkpoint is for; this is the clean-exit path and costs one write per backgrounding.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { tourViewModel.checkpoint() }

    /**
     * Ends a chapter and puts the learner back where it started.
     *
     * popUpTo the entry route unwinds everything the chapter pushed in one transaction, so someone
     * who skips halfway through the Settings chapter lands back on the help page rather than three
     * screens deep in a flow they did not choose.
     */
    val returnFromTour: (String) -> Unit = { entryRoute ->
        navController.navigate(entryRoute) {
            popUpTo(entryRoute) { inclusive = false }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Hoisted so startChapter (below) can resume a chapter where it was left, not only replay it
    // from the top: the Help page shows "Continue - step N of M" for exactly this reason, and until
    // this was threaded through, tapping that row silently restarted at step 0 regardless of what the
    // text said.
    val resumePoint by tourViewModel.resumePoint.collectAsState()

    val startChapter: (TourChapterId) -> Unit = { id ->
        val resumeStep = resumePoint?.takeIf { it.chapterId == id }?.step
        tourViewModel.startChapter(id, entryRoute = currentRoute, at = resumeStep ?: 0)
    }

    /** Replays the core chapter. Learn is rebuilt rather than restored, so stop 1's anchor is on
     *  screen instead of wherever the learner had last scrolled to. */
    val replayTour: () -> Unit = {
        tourViewModel.restart()
        navController.navigate(Screen.Learn.route) {
            popUpTo(Screen.Learn.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    CompositionLocalProvider(LocalTourAnchors provides tourAnchors) {
    Box(
        modifier = Modifier.fillMaxSize().background(Ground)
    ) {
        // The navigation cluster measures itself and insets the host by exactly its own height.
        //
        // Insetting is what actually prevents overlap: content padding inside a screen only adds
        // scroll room at the end, so a short screen still had the raised action sitting on top of it.
        // The height is measured rather than hardcoded because it depends on the gesture-navigation
        // inset, which varies by device and by whether three-button navigation is in use.
        var navClusterHeight by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current

        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) navClusterHeight else 0.dp)
                // While the tour is up, everything behind the dim is out of reach for a pointer, so
                // it must be out of reach for TalkBack too - otherwise swipe traversal wanders
                // through controls the learner cannot actually activate.
                .then(if (activeStop != null) Modifier.clearAndSetSemantics { } else Modifier)
        ) {
            // Splash Screen for routing
            composable(Screen.Splash.route) {
                val viewModel: SplashViewModel = hiltViewModel()
                val startDestination by viewModel.startDestination.collectAsState()
                
                LaunchedEffect(startDestination) {
                    if (startDestination != null) {
                        navController.navigate(startDestination!!) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                        // Notification deep link (Phase 5): open the target screen
                        // after routing (invalid routes are ignored safely).
                        if (!initialDeepLink.isNullOrBlank()) {
                            runCatching { navController.navigate(initialDeepLink) }
                        }
                    }
                }
            }

            // Onboarding Setup Wizard (Duolingo-style FTUE)
            composable(Screen.Onboarding.route) {
                val viewModel: com.kasiguru.ui.screens.onboarding.OnboardingViewModel = hiltViewModel()
                OnboardingScreen(
                    onCompleteOnboarding = { userName, avatarId, dailyGoalXp, motivation, startingLevel, titleBadge, residentName ->
                        viewModel.completeOnboarding(userName, avatarId, dailyGoalXp, titleBadge, residentName)
                        navController.navigate(Screen.Learn.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // "Who's learning?" - only ever reached when more than one profile exists (see
            // SplashViewModel), or from Settings to add/switch profiles on a shared device.
            composable(Screen.ProfileSelection.route) {
                com.kasiguru.ui.screens.profile.ProfileSelectionScreen(
                    onProfileSelected = {
                        navController.navigate(Screen.Learn.route) {
                            popUpTo(Screen.ProfileSelection.route) { inclusive = true }
                        }
                    },
                    onBack = if (navController.previousBackStackEntry != null) {
                        { navController.popBackStack() }
                    } else null
                )
            }

            // Learn: today's plan. Replaces the old Home dashboard, which duplicated four tabs.
            composable(Screen.Learn.route) {
                LearnScreen(
                    onStartLesson = { unitId, lessonIndex ->
                        navController.navigate(Screen.LessonPlayer.createRoute(unitId, lessonIndex))
                    },
                    onOpenReview = { navController.navigate(Screen.FlashcardDeck.route) },
                    onOpenGames = { navController.navigate(Screen.GameHub.route) },
                    onOpenStories = { navController.navigate(Screen.StoryList.route) },
                    onOpenDictionary = { navController.navigate(Screen.VocabularyList.route) },
                    onOpenProgress = { navController.navigate(Screen.Achievements.route) },
                    onOpenNotifications = { navController.navigate(Screen.Notifications.route) },
                    onOpenProfile = { navController.navigate(Screen.Profile.route) },
                    onOpenAccount = { navController.navigate(Screen.Account.route) }
                )
            }

            // The lesson player. Immersive: no bottom bar, no FAB.
            composable(
                route = Screen.LessonPlayer.route,
                arguments = listOf(
                    navArgument("unitId") { type = NavType.StringType },
                    navArgument("lessonIndex") { type = NavType.IntType }
                )
            ) {
                LessonPlayerScreen(
                    onExit = { navController.popBackStack() },
                    // Finishing returns to Learn, which re-derives today's plan so the completed
                    // lesson is replaced by the next one rather than sitting there still marked to do.
                    onFinished = { navController.popBackStack() }
                )
            }

            // Profile
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                    onNavigateToLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                    onNavigateToCultural = { navController.navigate(Screen.CulturalContext.route) },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) },
                    onNavigateToHelp = { navController.navigate(Screen.Help.route) }
                )
            }
            
            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Learn (Vocabulary & Dictionary)
            composable(Screen.VocabularyList.route) {
                VocabularyScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCategory = { category ->
                        navController.navigate(Screen.VocabularyCategory.createRoute(category))
                    },
                    onNavigateToWord = { wordId ->
                        navController.navigate(Screen.VocabularyDetail.createRoute(wordId))
                    },
                    onNavigateToSubmitWord = { navController.navigate(Screen.SubmitWord.route) }
                )
            }

            composable(
                route = Screen.VocabularyCategory.route,
                arguments = listOf(navArgument("category") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryName = backStackEntry.arguments?.getString("category") ?: "All"
                CategoryDetailScreen(
                    categoryName = categoryName,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToWord = { wordId ->
                        navController.navigate(Screen.VocabularyDetail.createRoute(wordId))
                    }
                )
            }

            composable(
                route = Screen.VocabularyDetail.route,
                arguments = listOf(navArgument("wordId") { type = NavType.IntType })
            ) {
                VocabularyDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onReportWord = { word ->
                        navController.navigate(
                            Screen.ReportIssue.createRoute(
                                category = "Wrong Word / Translation",
                                word = word,
                                screenContext = "Vocabulary"
                            )
                        )
                    }
                )
            }

            // Stories
            composable(Screen.StoryList.route) {
                StoryListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToStory = { storyId ->
                        navController.navigate(Screen.StoryReader.createRoute(storyId))
                    },
                    onNavigateToSubmitLiterature = { navController.navigate(Screen.SubmitLiterature.route) }
                )
            }
            composable(
                route = Screen.StoryReader.route,
                arguments = listOf(navArgument("storyId") { type = NavType.IntType })
            ) {
                StoryReaderScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.GameHub.route) {
                GameHubScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLevelSelection = { gameType -> 
                        navController.navigate(Screen.LevelSelection.createRoute(gameType)) 
                    }
                )
            }
            
            composable(
                route = Screen.LevelSelection.route,
                arguments = listOf(navArgument("gameType") { type = NavType.StringType })
            ) {
                LevelSelectionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToGame = { gameType, level ->
                        when (gameType) {
                            "word_match" -> navController.navigate(Screen.WordMatchGame.createRoute(level))
                            "reverse_match" -> navController.navigate(Screen.ReverseMatchGame.createRoute(level))
                            "fill_blank" -> navController.navigate(Screen.FillBlankGame.createRoute(level))
                            Constants.Games.RECALL -> navController.navigate(Screen.RecallGame.createRoute(level))
                            "aspect_builder" -> navController.navigate(Screen.AspectBuilderGame.createRoute(level))
                            "sentence_order" -> navController.navigate(Screen.SentenceOrderGame.createRoute(level))
                        }
                    }
                )
            }
            
            composable(
                route = Screen.WordMatchGame.route,
                arguments = listOf(navArgument("level") { type = NavType.IntType })
            ) {
                WordMatchGameScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNextLevel = { nextLevel ->
                        navController.navigate(Screen.WordMatchGame.createRoute(nextLevel)) {
                            popUpTo(Screen.WordMatchGame.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Screen.ReverseMatchGame.route,
                arguments = listOf(navArgument("level") { type = NavType.IntType })
            ) {
                ReverseMatchGameScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNextLevel = { nextLevel ->
                        navController.navigate(Screen.ReverseMatchGame.createRoute(nextLevel)) {
                            popUpTo(Screen.ReverseMatchGame.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Screen.FillBlankGame.route,
                arguments = listOf(navArgument("level") { type = NavType.IntType })
            ) {
                FillBlankGameScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNextLevel = { nextLevel ->
                        navController.navigate(Screen.FillBlankGame.createRoute(nextLevel)) {
                            popUpTo(Screen.FillBlankGame.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Screen.RecallGame.route,
                arguments = listOf(navArgument("level") { type = NavType.IntType })
            ) {
                RecallGameScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNextLevel = { nextLevel ->
                        navController.navigate(Screen.RecallGame.createRoute(nextLevel)) {
                            popUpTo(Screen.RecallGame.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Screen.AspectBuilderGame.route,
                arguments = listOf(navArgument("level") { type = NavType.IntType })
            ) {
                AspectBuilderGameScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNextLevel = { nextLevel ->
                        navController.navigate(Screen.AspectBuilderGame.createRoute(nextLevel)) {
                            popUpTo(Screen.AspectBuilderGame.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Screen.SentenceOrderGame.route,
                arguments = listOf(navArgument("level") { type = NavType.IntType })
            ) {
                SentenceOrderGameScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNextLevel = { nextLevel ->
                        navController.navigate(Screen.SentenceOrderGame.createRoute(nextLevel)) {
                            popUpTo(Screen.SentenceOrderGame.route) { inclusive = true }
                        }
                    }
                )
            }

            // Profile & Achievements
            composable(Screen.Achievements.route) {
                AchievementsScreen(onNavigateBack = { navController.popBackStack() })
            }

            // Review (Daily Spaced-Repetition Deck)
            composable(Screen.FlashcardDeck.route) {
                FlashcardDeckScreen(onNavigateBack = { navController.popBackStack() })
            }

            // Expanded Inventory Screens
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAccount = { navController.navigate(Screen.Account.route) },
                    onNavigateToProfiles = { navController.navigate(Screen.ProfileSelection.route) },
                    onNavigateToReport = { navController.navigate(Screen.ReportIssue.createRoute()) },
                    onReplayTutorial = replayTour
                )
            }
            composable(Screen.Account.route) {
                AccountScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.CulturalContext.route) {
                CulturalScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Leaderboard.route) {
                LeaderboardScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Notifications.route) {
                NotificationInboxScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRoute = { route -> navController.navigate(route) }
                )
            }
            composable(Screen.About.route) {
                AboutScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.Help.route) {
                val chapterStates by tourViewModel.chapterStates.collectAsState()
                HowToUseScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onReplayTour = replayTour,
                    chapterStates = chapterStates,
                    resumePoint = resumePoint,
                    onStartChapter = startChapter,
                    onNavigateToSubmitWord = { navController.navigate(Screen.SubmitWord.route) },
                    onNavigateToReport = {
                        navController.navigate(Screen.ReportIssue.createRoute(screenContext = "Help"))
                    }
                )
            }

            composable(Screen.SubmitWord.route) {
                SubmitWordScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.SubmitLiterature.route) {
                SubmitLiteratureScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.ReportIssue.route,
                arguments = listOf(
                    navArgument("category") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("word") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("screenContext") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val category = backStackEntry.arguments?.getString("category")
                val word = backStackEntry.arguments?.getString("word")
                val screenContext = backStackEntry.arguments?.getString("screenContext")
                ReportIssueScreen(
                    prefilledCategory = category,
                    prefilledWord = word,
                    prefilledScreenContext = screenContext,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        if (showBottomBar) {
            KasiGuruBottomBar(
                currentRoute = currentRoute,
                onNavigateToRoute = switchTab,
                // Still measured, and still worth measuring: the cluster is shorter without the docked
                // FAB, so every screen's bottom inset shrinks with it rather than being hardcoded.
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size ->
                        navClusterHeight = with(density) { size.height.toDp() }
                    }
                    .then(if (activeStop != null) Modifier.clearAndSetSemantics { } else Modifier)
            )
        }

        // Last child of the Box on purpose: it has to dim and cut through the floating bar as well
        // as the nav host, and it shares their coordinate space, which a Dialog would not.
        activeTour?.takeIf { tourAllowed }?.let { tour ->
            val stop = tour.current ?: return@let
            SpotlightOverlay(
                stop = stop.stop,
                chapterTitle = tour.chapter.title,
                stepIndex = tour.index,
                stepCount = tour.stops.size,
                // The registry is handed over rather than read here. Reading it in this scope would
                // make every anchor measurement recompose the whole navigation graph, and a relayout
                // that re-reports its bounds turns that into a loop.
                anchors = tourAnchors,
                // Only trust the anchor once the destination it lives on is actually showing;
                // otherwise a hole would be cut at coordinates the previous screen reported.
                anchorVisible = currentRoute == stop.route,
                bottomBlocked = if (showBottomBar) navClusterHeight else 0.dp,
                onBack = {
                    val wasFirst = tour.index == 0
                    tourViewModel.back()
                    if (wasFirst) returnFromTour(tour.entryRoute)
                },
                onSkip = {
                    tourViewModel.skip()
                    returnFromTour(tour.entryRoute)
                },
                onNext = {
                    val wasLast = tour.isLast
                    tourViewModel.next()
                    if (wasLast) returnFromTour(tour.entryRoute)
                }
            )
        }
    }
    }
}
