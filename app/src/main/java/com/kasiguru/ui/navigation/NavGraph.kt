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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
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
import com.kasiguru.ui.screens.about.AboutScreen
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

    val showBottomBar = currentRoute in listOf(
        Screen.Learn.route,
        Screen.GameHub.route,
        Screen.VocabularyList.route,
        Screen.Achievements.route,
        Screen.Profile.route
    )

    val levelUpViewModel: LevelUpViewModel = hiltViewModel()
    val pendingLevelUp by levelUpViewModel.pendingLevelUp.collectAsState()
    pendingLevelUp?.let { levelInfo ->
        LevelUpDialog(newLevelInfo = levelInfo, onDismiss = levelUpViewModel::dismiss)
    }

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
            modifier = Modifier.fillMaxSize().padding(bottom = if (showBottomBar) navClusterHeight else 0.dp)
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
                    onNavigateToAbout = { navController.navigate(Screen.About.route) }
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
                    onNavigateBack = { navController.popBackStack() }
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
                WordMatchGameScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.ReverseMatchGame.route,
                arguments = listOf(navArgument("level") { type = NavType.IntType })
            ) {
                ReverseMatchGameScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.FillBlankGame.route,
                arguments = listOf(navArgument("level") { type = NavType.IntType })
            ) {
                FillBlankGameScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.RecallGame.route,
                arguments = listOf(navArgument("level") { type = NavType.IntType })
            ) {
                RecallGameScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.AspectBuilderGame.route,
                arguments = listOf(navArgument("level") { type = NavType.IntType })
            ) {
                AspectBuilderGameScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.SentenceOrderGame.route,
                arguments = listOf(navArgument("level") { type = NavType.IntType })
            ) {
                SentenceOrderGameScreen(onNavigateBack = { navController.popBackStack() })
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
                    onNavigateToProfiles = { navController.navigate(Screen.ProfileSelection.route) }
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

            composable(Screen.SubmitWord.route) {
                SubmitWordScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.SubmitLiterature.route) {
                SubmitLiteratureScreen(onNavigateBack = { navController.popBackStack() })
            }
        }

        if (showBottomBar) {
            KasiGuruBottomBar(
                currentRoute = currentRoute,
                onNavigateToRoute = { route ->
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(Screen.Learn.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                // Still measured, and still worth measuring: the cluster is shorter without the docked
                // FAB, so every screen's bottom inset shrinks with it rather than being hardcoded.
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size ->
                        navClusterHeight = with(density) { size.height.toDp() }
                    }
            )
        }
    }
}
