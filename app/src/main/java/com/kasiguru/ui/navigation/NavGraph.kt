package com.kasiguru.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.Ground
import com.kasiguru.ui.theme.LocalBottomBarInset
import com.kasiguru.ui.theme.LocalFloatingNavBarVisible
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.kasiguru.ui.components.RewardCelebrationDialog
import com.kasiguru.ui.components.StreakCelebrationDialog
import com.kasiguru.ui.components.AppUpdateDialog
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
import com.kasiguru.ui.screens.games.aspectbuilder.AspectBuilderGameScreen
import com.kasiguru.ui.screens.games.fillblank.FillBlankGameScreen
import com.kasiguru.ui.screens.games.hub.GameHubScreen
import com.kasiguru.ui.screens.games.levels.LevelSelectionScreen
import com.kasiguru.ui.screens.games.recall.RecallGameScreen
import com.kasiguru.ui.screens.games.reversematch.ReverseMatchGameScreen
import com.kasiguru.ui.screens.games.sentenceorder.SentenceOrderGameScreen
import com.kasiguru.ui.screens.games.wordmatch.WordMatchGameScreen
import com.kasiguru.ui.screens.games.wordsearch.WordSearchCategoryScreen
import com.kasiguru.ui.screens.games.wordsearch.WordSearchGameScreen
import com.kasiguru.ui.screens.games.wordwheel.WordWheelGameScreen
import com.kasiguru.ui.screens.home.HomeScreen
import com.kasiguru.ui.screens.learn.LearnScreen
import com.kasiguru.ui.screens.library.LibraryScreen
import com.kasiguru.ui.screens.library.LibrarySegment
import com.kasiguru.ui.screens.lesson.LessonPlayerScreen
import com.kasiguru.ui.screens.onboarding.OnboardingScreen
import com.kasiguru.ui.screens.contribute.SubmitLiteratureScreen
import com.kasiguru.ui.screens.contribute.SubmitWordScreen
import com.kasiguru.ui.screens.profile.EditProfileScreen
import com.kasiguru.ui.screens.profile.ProfileScreen
import com.kasiguru.ui.screens.report.ReportIssueScreen
import com.kasiguru.ui.screens.settings.SettingsScreen
import com.kasiguru.ui.screens.stories.StoriesComingSoonScreen
import com.kasiguru.ui.screens.stories.StoryListScreen
import com.kasiguru.ui.screens.stories.StoryReaderScreen
import com.kasiguru.ui.screens.vocabulary.CategoryDetailScreen
import com.kasiguru.ui.screens.vocabulary.VocabularyDetailScreen
import com.kasiguru.ui.screens.vocabulary.VocabularyScreen
import com.kasiguru.ui.screens.banned.AccountSuspendedScreen
import com.kasiguru.ui.screens.banned.BanCheckViewModel
import com.kasiguru.ui.screens.banned.BanState
import com.kasiguru.util.Constants
import com.kasiguru.domain.audio.musicMoodFor

@Composable
fun KasiGuruNavGraph(initialDeepLink: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in Screen.tabRoots

    val musicViewModel: MusicViewModel = hiltViewModel()
    LaunchedEffect(currentRoute) { musicViewModel.setMood(musicMoodFor(currentRoute)) }

    val rewardViewModel: RewardCelebrationViewModel = hiltViewModel()
    val pendingReward by rewardViewModel.pending.collectAsState()
    pendingReward?.let { reward ->
        RewardCelebrationDialog(reward,onDismiss = rewardViewModel::dismiss)
    }

    val streakCelebrationViewModel: StreakCelebrationViewModel = hiltViewModel()
    val pendingStreakActivation by streakCelebrationViewModel.pendingStreakActivation.collectAsState()
    pendingStreakActivation?.let { streakDays ->
        if(pendingReward == null) StreakCelebrationDialog(streakDays = streakDays, onDismiss = streakCelebrationViewModel::dismiss)
    }

    // A new version. A required one covers every screen; an optional one waits for a tab and for
    // any celebration to finish, so it never lands on onboarding or on top of a reward.
    val appUpdateViewModel: AppUpdatePromptViewModel = hiltViewModel()
    val appUpdate by appUpdateViewModel.state.collectAsState()
    appUpdate.release?.let { release ->
        val calm = pendingReward == null && pendingStreakActivation == null && currentRoute in Screen.tabRoots
        if (appUpdate.showDialog && (release.forceUpdate || calm)) {
            AppUpdateDialog(release = release, onLater = appUpdateViewModel::later)
        }
    }

    // ── Ban gate ─────────────────────────────────────────────────────────────────
    // One-shot check at startup. If the current signed-in account has an active ban
    // written by an admin, we redirect immediately to AccountSuspendedScreen and
    // clear the back stack so the user cannot navigate away from it.
    val banCheckViewModel: BanCheckViewModel = hiltViewModel()
    val banState by banCheckViewModel.banState.collectAsState()
    val isSigningOut by banCheckViewModel.isSigningOut.collectAsState()
    val isSubmittingAppeal by banCheckViewModel.isSubmittingAppeal.collectAsState()
    val appealError by banCheckViewModel.appealError.collectAsState()

    LaunchedEffect(banState, isSigningOut) {
        if (isSigningOut) return@LaunchedEffect
        if (banState is BanState.Banned && currentRoute != Screen.AccountSuspended.route) {
            navController.navigate(Screen.AccountSuspended.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        } else if (banState is BanState.Clear && currentRoute == Screen.AccountSuspended.route) {
            navController.navigate(Screen.Splash.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // Moving between the five tab roots. Hoisted out of the bottom bar's own call site because the
    // guided tour drives the same navigation, and it must do it with exactly these options: a bare
    // navigate() would leave Home→Learn→Practice→Library→Me on the back stack, so the first Back
    // after a tour would walk the learner backwards through five screens they never chose.
    val switchTab: (String) -> Unit = { route ->
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(Screen.Home.route) { saveState = true }
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
        Screen.ProfileSelection.route,
        Screen.AccountSuspended.route
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

    /** Replays the core chapter. Home is rebuilt rather than restored, so stop 1's anchor is on
     *  screen instead of wherever the learner had last scrolled to. */
    val replayTour: () -> Unit = {
        tourViewModel.restart()
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Home.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    // A Library side another screen asked for (Home's "See all" stories). Held here because the
    // request crosses a tab switch; LibraryScreen clears it once applied.
    var librarySegmentRequest by rememberSaveable { mutableStateOf<Int?>(null) }
    val openLibraryStories: () -> Unit = {
        librarySegmentRequest = LibrarySegment.STORIES
        switchTab(Screen.Library.route)
    }
    val openLibraryMyWords: () -> Unit = {
        librarySegmentRequest = LibrarySegment.MY_WORDS
        switchTab(Screen.Library.route)
    }

    CompositionLocalProvider(LocalTourAnchors provides tourAnchors) {
    Box(
        modifier = Modifier.fillMaxSize().background(Ground)
    ) {
        // The navigation bar floats over the screens instead of insetting them. Insetting the host by
        // the bar's height left a solid strip of ground behind the pill, so content stopped short of
        // a block rather than passing under a floating bar. Screens scroll on underneath it, and
        // Space.navBarClearance adds the bar's measured height to their end padding so the last item
        // still clears it. Measured, not hardcoded: it depends on the gesture-navigation inset, which
        // varies by device and by whether three-button navigation is in use.
        var navClusterHeight by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current

        CompositionLocalProvider(
            LocalBottomBarInset provides if (showBottomBar) navClusterHeight else 0.dp,
            LocalFloatingNavBarVisible provides showBottomBar
        ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier
                .fillMaxSize()
                // In landscape, three-button navigation and the camera cutout sit on a side edge.
                // Every screen stays clear of them sideways; the ground behind still runs edge to edge.
                .windowInsetsPadding(
                    WindowInsets.navigationBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
                )
                // While the tour is up, everything behind the dim is out of reach for a pointer, so
                // it must be out of reach for TalkBack too - otherwise swipe traversal wanders
                // through controls the learner cannot actually activate.
                .then(if (activeStop != null) Modifier.clearAndSetSemantics { } else Modifier)
        ) {
            // Splash Screen for routing
            composable(Screen.Splash.route) {
                val viewModel: SplashViewModel = hiltViewModel()
                val startDestination by viewModel.startDestination.collectAsState()
                // The same plain green as the system splash, so the hand-off to the first real
                // screen is one cut from green rather than green, then a dark frame, then the app.
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color(0xFF6CB619))
                )

                LaunchedEffect(startDestination) {
                    if (startDestination != null) {
                        // SplashViewModel still names Learn as the everyday landing; the landing is
                        // Home now. Mapped here so the entry point is right whichever way that
                        // file reads.
                        val target = startDestination!!.let {
                            if (it == Screen.Learn.route) Screen.Home.route else it
                        }
                        navController.navigate(target) {
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
                    onCompleteOnboarding = { userName, avatarId, dailyGoalXp, titleBadge, residentName ->
                        viewModel.completeOnboarding(userName, avatarId, dailyGoalXp, titleBadge, residentName)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                    onOpenLogin = {
                        navController.navigate(Screen.Account.route)
                    }
                )
            }

            // "Who's learning?" - only ever reached when more than one profile exists (see
            // SplashViewModel), or from Settings to add/switch profiles on a shared device.
            composable(Screen.ProfileSelection.route) {
                com.kasiguru.ui.screens.profile.ProfileSelectionScreen(
                    onProfileSelected = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.ProfileSelection.route) { inclusive = true }
                        }
                    },
                    onBack = if (navController.previousBackStackEntry != null) {
                        { navController.popBackStack() }
                    } else null
                )
            }

            // Home: today's one next action, the day goal, reviews, stories and the week.
            composable(Screen.Home.route) {
                HomeScreen(
                    updateRelease = appUpdate.release?.takeIf { appUpdate.showBanner },
                    onOpenUpdate = appUpdateViewModel::openDialog,
                    onDismissUpdate = appUpdateViewModel::dismissBanner,
                    onStartLesson = { unitId, lessonIndex ->
                        navController.navigate(Screen.LessonPlayer.createRoute(unitId, lessonIndex))
                    },
                    onOpenReview = { navController.navigate(Screen.FlashcardDeck.route) },
                    // Tabs are switched to, never pushed: a pushed tab root would sit on top of Home
                    // with the bar showing, and Back would unwind it like a detail screen.
                    onOpenGames = { switchTab(Screen.GameHub.route) },
                    onOpenStories = openLibraryStories,
                    onOpenMyWords = openLibraryMyWords,
                    onOpenStory = { storyId ->
                        navController.navigate(Screen.StoryReader.createRoute(storyId))
                    },
                    onOpenProgress = { navController.navigate(Screen.Achievements.route) },
                    onOpenNotifications = { navController.navigate(Screen.Notifications.route) },
                    onOpenProfile = { switchTab(Screen.Profile.route) },
                    onOpenAccount = { navController.navigate(Screen.Account.route) },
                    onOpenStreak = { navController.navigate(Screen.Streak.route) },
                    onOpenXp = { navController.navigate(Screen.Xp.route) },
                    onOpenWord = { navController.navigate(Screen.VocabularyDetail.createRoute(it)) },
                    onOpenGame = { game ->
                        navController.navigate(if (game == Constants.Games.WORD_SEARCH) Screen.WordSearchCategories.route else Screen.LevelSelection.createRoute(game))
                    },
                    onOpenSubmitWord = { navController.navigate(Screen.SubmitWord.route) },
                    onOpenReport = { navController.navigate(Screen.ReportIssue.createRoute(screenContext = "Home")) }
                )
            }

            composable(Screen.Streak.route) {
                com.kasiguru.ui.screens.streak.StreakScreen(
                    onBack = { navController.popBackStack() },
                    onContinue = { navController.popBackStack(); switchTab(Screen.Home.route) }
                )
            }
            composable(Screen.Xp.route) {
                com.kasiguru.ui.screens.xp.XpScreen(
                    onBack = { navController.popBackStack() },
                    onContinue = { navController.popBackStack(); switchTab(Screen.Home.route) },
                    onOpenGames = { navController.popBackStack(); switchTab(Screen.GameHub.route) }
                )
            }

            // Learn: the learning path alone.
            composable(Screen.Learn.route) {
                LearnScreen(
                    onStartLesson = { unitId, lessonIndex ->
                        navController.navigate(Screen.LessonPlayer.createRoute(unitId, lessonIndex))
                    }
                )
            }

            // Library: Words and Stories behind one toggle.
            composable(Screen.Library.route) {
                LibraryScreen(
                    onNavigateToCategory = { category ->
                        navController.navigate(Screen.VocabularyCategory.createRoute(category))
                    },
                    onNavigateToWord = { wordId ->
                        navController.navigate(Screen.VocabularyDetail.createRoute(wordId))
                    },
                    onNavigateToAddWord = { navController.navigate(Screen.SubmitWord.route) },
                    onNavigateToStory = { storyId ->
                        navController.navigate(Screen.StoryReader.createRoute(storyId))
                    },
                    onNavigateToShareStory = { navController.navigate(Screen.SubmitLiterature.route) },
                    onOpenReview = { navController.navigate(Screen.FlashcardDeck.route) },
                    segmentRequest = librarySegmentRequest,
                    onSegmentRequestConsumed = { librarySegmentRequest = null }
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
                    // Finishing returns to Home or Learn, both of which re-derive on resume, so the
                    // completed lesson is replaced by the next one rather than still marked to do.
                    onFinished = { showStreak ->
                        navController.popBackStack()
                        if (showStreak) navController.navigate(Screen.Streak.route)
                    }
                )
            }

            // Me. The route keeps its old name so deep links written against it still land.
            composable(Screen.PublicProfile.route, arguments = listOf(navArgument("uid") { type = NavType.StringType })) { entry ->
                val uid = entry.arguments?.getString("uid").orEmpty()
                com.kasiguru.ui.screens.profile.PublicProfileScreen(onBack = { navController.popBackStack() },
                    onReport = { navController.navigate(Screen.ReportIssue.createRoute(category = "Other", screenContext = "Public player profile: $uid")) })
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                    onNavigateToCultural = { navController.navigate(Screen.CulturalContext.route) },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) },
                    onNavigateToHelp = { navController.navigate(Screen.Help.route) },
                    onNavigateToAccount = { navController.navigate(Screen.Account.route) },
                    onNavigateToLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                    onNavigateToStreak = { navController.navigate(Screen.Streak.route) }
                )
            }
            
            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // The dictionary on its own, pushed (deep links, the dictionary tour). The Library tab
            // shows the same content.
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
                    onOpenWord = { wordId ->
                        navController.navigate(Screen.VocabularyDetail.createRoute(wordId))
                    },
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

            // The stories list on its own, pushed. The Library tab shows the same content.
            // While stories are switched off, both routes - reached from notifications, help and the
            // Story Reader badge - say they are coming soon instead of opening an unnarrated story.
            composable(Screen.StoryList.route) {
                if (Constants.STORIES_ENABLED) {
                    StoryListScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToStory = { storyId ->
                            navController.navigate(Screen.StoryReader.createRoute(storyId))
                        },
                        onNavigateToSubmitLiterature = { navController.navigate(Screen.SubmitLiterature.route) }
                    )
                } else {
                    StoriesComingSoonScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onOpenMyWords = openLibraryMyWords
                    )
                }
            }
            composable(
                route = Screen.StoryReader.route,
                arguments = listOf(navArgument("storyId") { type = NavType.IntType })
            ) {
                if (Constants.STORIES_ENABLED) {
                    StoryReaderScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                } else {
                    StoriesComingSoonScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onOpenMyWords = openLibraryMyWords
                    )
                }
            }

            composable(Screen.GameHub.route) {
                GameHubScreen(
                    onOpenPlayer = { navController.navigate(Screen.PublicProfile.createRoute(it)) },
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                    onNavigateToLevelSelection = { gameType ->
                        // Word Search asks for a category before any levels: its levels belong to one.
                        if (gameType == Constants.Games.WORD_SEARCH) {
                            navController.navigate(Screen.WordSearchCategories.route)
                        } else {
                            navController.navigate(Screen.LevelSelection.createRoute(gameType))
                        }
                    }
                )
            }

            composable(
                route = Screen.WordWheelGame.route,
                arguments = listOf(navArgument("level") { type = NavType.IntType })
            ) {
                WordWheelGameScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNextLevel = { nextLevel ->
                        navController.navigate(Screen.WordWheelGame.createRoute(nextLevel)) {
                            popUpTo(Screen.WordWheelGame.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.WordSearchCategories.route) {
                WordSearchCategoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onCategorySelected = { levelKey ->
                        navController.navigate(Screen.LevelSelection.createRoute(levelKey))
                    }
                )
            }
            composable(
                route = Screen.WordSearchGame.route,
                arguments = listOf(
                    navArgument("category") { type = NavType.StringType },
                    navArgument("level") { type = NavType.IntType }
                )
            ) { entry ->
                val category = entry.arguments?.getString("category").orEmpty()
                WordSearchGameScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNextLevel = { nextLevel ->
                        navController.navigate(Screen.WordSearchGame.createRoute(category, nextLevel)) {
                            popUpTo(Screen.WordSearchGame.route) { inclusive = true }
                        }
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
                            Constants.Games.WORD_WHEEL -> navController.navigate(Screen.WordWheelGame.createRoute(level))
                            else -> if (Constants.Games.isWordSearchLevelKey(gameType)) {
                                navController.navigate(Screen.WordSearchGame.createRoute(gameType, level))
                            }
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

            // Badges, pushed from Me and from Home's goal ring.
            composable(Screen.Achievements.route) {
                AchievementsScreen(onNavigateBack = { navController.popBackStack() },
                    onNavigateToActivity = { family ->
                        val route = when(family) {
                            "word_explorer","review_keeper" -> Screen.FlashcardDeck.route
                            "game_adventurer","precision_player","mode_explorer" -> Screen.GameHub.route
                            "story_reader" -> Screen.StoryList.route
                            "category_scholar" -> Screen.VocabularyList.route
                            "community_contributor" -> Screen.SubmitWord.route
                            "consistent_learner" -> Screen.Home.route
                            else -> Screen.Learn.route
                        }
                        navController.navigate(route) { launchSingleTop = true }
                    })
            }

            // Review (Daily Spaced-Repetition Deck)
            composable(Screen.FlashcardDeck.route) {
                FlashcardDeckScreen(onNavigateBack = { navController.popBackStack() })
            }

            // Expanded Inventory Screens
            composable(Screen.Appearance.route) {
                com.kasiguru.ui.screens.settings.AppearanceScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Settings.route) {
                val chapterStates by tourViewModel.chapterStates.collectAsState()
                val resumePoint by tourViewModel.resumePoint.collectAsState()
                SettingsScreen(
                    chapterStates = chapterStates, resumePoint = resumePoint, onStartChapter = startChapter,
                    onOpenAppearance = { navController.navigate(Screen.Appearance.route) },
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAccount = { navController.navigate(Screen.Account.route) },
                    onNavigateToProfiles = { navController.navigate(Screen.ProfileSelection.route) },
                    onNavigateToReport = { navController.navigate(Screen.ReportIssue.createRoute()) },
                    onReplayTutorial = replayTour
                )
            }
            composable(Screen.Account.route) {
                AccountScreen(
                    returningLearner = navController.previousBackStackEntry?.destination?.route ==
                        Screen.Onboarding.route,
                    onNavigateBack = { navController.popBackStack() },
                    onAuthSuccess = {
                        val previousRoute = navController.previousBackStackEntry?.destination?.route
                        if (previousRoute == Screen.Onboarding.route) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                    // The previous person's screens must not survive under the next one's, so both
                    // start over with the whole back stack cleared.
                    onAccountSwitched = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    // Sign-out reseeds the progress row, so the next learner gets the first-run wizard.
                    onSignedOut = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.CulturalContext.route) {
                CulturalScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Leaderboard.route) {
                LeaderboardScreen(onNavigateBack = { navController.popBackStack() }, onOpenPlayer = { navController.navigate(Screen.PublicProfile.createRoute(it)) })
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

            // Account Suspended - shown when the admin has banned the current account.
            // No back navigation: the back stack is cleared when we navigate here.
            composable(Screen.AccountSuspended.route) {
                val state = banState
                if (state is BanState.Banned) {
                    AccountSuspendedScreen(
                        reason = state.reason,
                        bannedAt = state.bannedAt,
                        appealText = state.appealText,
                        appealSubmittedAt = state.appealSubmittedAt,
                        appealStatus = state.appealStatus,
                        appealReviewNotes = state.appealReviewNotes,
                        appealReviewedAt = state.appealReviewedAt,
                        isSubmittingAppeal = isSubmittingAppeal,
                        appealError = appealError,
                        onSubmitAppeal = { text -> banCheckViewModel.submitAppeal(text) },
                        onClearAppealError = { banCheckViewModel.clearAppealError() },
                        onSignOut = {
                            banCheckViewModel.signOut {
                                navController.navigate(Screen.Splash.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        isSigningOut = isSigningOut
                    )
                }
            }
        } // end NavHost
        }

        if (showBottomBar) {
            KasiGuruBottomBar(
                currentRoute = currentRoute,
                onNavigateToRoute = switchTab,
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
                anchors = tourAnchors,
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
