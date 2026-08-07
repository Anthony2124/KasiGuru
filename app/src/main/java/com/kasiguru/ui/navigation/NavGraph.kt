package com.kasiguru.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.kasiguru.ui.screens.about.AboutScreen
import com.kasiguru.ui.screens.achievements.AchievementsScreen
import com.kasiguru.ui.screens.auth.RegisterScreen
import com.kasiguru.ui.screens.auth.SplashViewModel
import com.kasiguru.ui.screens.auth.WelcomeScreen
import com.kasiguru.ui.screens.cultural.CulturalScreen
import com.kasiguru.ui.screens.flashcards.FlashcardDeckScreen
import com.kasiguru.ui.screens.leaderboard.LeaderboardScreen
import com.kasiguru.ui.screens.notifications.NotificationInboxScreen
import com.kasiguru.ui.screens.games.*
import com.kasiguru.ui.screens.home.HomeScreen
import com.kasiguru.ui.screens.onboarding.OnboardingScreen
import com.kasiguru.ui.screens.contribute.SubmitWordScreen
import com.kasiguru.ui.screens.profile.EditProfileScreen
import com.kasiguru.ui.screens.profile.ProfileScreen
import com.kasiguru.ui.screens.settings.SettingsScreen
import com.kasiguru.ui.screens.stories.StoryListScreen
import com.kasiguru.ui.screens.stories.StoryReaderScreen
import com.kasiguru.ui.screens.vocabulary.CategoryDetailScreen
import com.kasiguru.ui.screens.vocabulary.VocabularyScreen

@Composable
fun KasiGuruNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.VocabularyList.route,
        Screen.FlashcardDeck.route,
        Screen.GameHub.route,
        Screen.Profile.route
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.fillMaxSize()
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
                    }
                }
            }

            // Auth Flow: Welcome -> Register -> Home
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Onboarding Setup Wizard (Duolingo-style FTUE)
            composable(Screen.Onboarding.route) {
                val viewModel: com.kasiguru.ui.screens.onboarding.OnboardingViewModel = hiltViewModel()
                OnboardingScreen(
                    onCompleteOnboarding = { userName, avatarId, dailyGoalXp, motivation, startingLevel, titleBadge ->
                        viewModel.completeOnboarding(userName, avatarId, dailyGoalXp, titleBadge)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // Home Dashboard (Central Hub)
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToStories = { navController.navigate(Screen.StoryList.route) },
                    onNavigateToVocabulary = { navController.navigate(Screen.VocabularyList.route) },
                    onNavigateToGames = { navController.navigate(Screen.GameHub.route) },
                    onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                    onNavigateToLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                    onNavigateToCultural = { navController.navigate(Screen.CulturalContext.route) },
                    onNavigateToFlashcards = { navController.navigate(Screen.FlashcardDeck.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) },
                    onNavigateToSubmitWord = { navController.navigate(Screen.SubmitWord.route) }
                )
            }

            // Profile
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) }
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
                    }
                )
            }
            
            composable(
                route = Screen.VocabularyCategory.route,
                arguments = listOf(navArgument("category") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryName = backStackEntry.arguments?.getString("category") ?: "All"
                CategoryDetailScreen(
                    categoryName = categoryName,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            // Stories
            composable(Screen.StoryList.route) {
                StoryListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToStory = { storyId ->
                        navController.navigate(Screen.StoryReader.createRoute(storyId))
                    }
                )
            }
            composable(Screen.StoryReader.route) {
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
                            "fill_blank" -> navController.navigate(Screen.FillBlankGame.createRoute(level))
                            "audio_quiz" -> navController.navigate(Screen.AudioQuizGame.createRoute(level))
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
                route = Screen.FillBlankGame.route,
                arguments = listOf(navArgument("level") { type = NavType.IntType })
            ) {
                FillBlankGameScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.AudioQuizGame.route,
                arguments = listOf(navArgument("level") { type = NavType.IntType })
            ) {
                AudioQuizGameScreen(onNavigateBack = { navController.popBackStack() })
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
                SettingsScreen(onNavigateBack = { navController.popBackStack() })
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
        }

        if (showBottomBar) {
            KasiGuruBottomBar(
                currentRoute = currentRoute,
                onNavigateToRoute = { route ->
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
