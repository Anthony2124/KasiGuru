package com.kasiguru.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kasiguru.ui.components.KasiGuruBottomBar
import com.kasiguru.ui.screens.about.AboutScreen
import com.kasiguru.ui.screens.achievements.AchievementsScreen
import com.kasiguru.ui.screens.cultural.CulturalScreen
import com.kasiguru.ui.screens.flashcards.FlashcardDeckScreen
import com.kasiguru.ui.screens.games.AspectBuilderGameScreen
import com.kasiguru.ui.screens.games.AudioQuizGameScreen
import com.kasiguru.ui.screens.games.FillBlankGameScreen
import com.kasiguru.ui.screens.games.GameHubScreen
import com.kasiguru.ui.screens.games.SentenceOrderGameScreen
import com.kasiguru.ui.screens.games.WordMatchGameScreen
import com.kasiguru.ui.screens.home.HomeScreen
import com.kasiguru.ui.screens.onboarding.OnboardingScreen
import com.kasiguru.ui.screens.profile.EditProfileScreen
import com.kasiguru.ui.screens.profile.ProfileScreen
import com.kasiguru.ui.screens.settings.SettingsScreen
import com.kasiguru.ui.screens.stories.StoryListScreen
import com.kasiguru.ui.screens.stories.StoryReaderScreen
import com.kasiguru.ui.screens.vocabulary.VocabularyScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.screens.auth.LoginScreen
import com.kasiguru.ui.screens.auth.RegisterScreen
import com.kasiguru.ui.screens.auth.SplashViewModel
import com.kasiguru.ui.screens.auth.WelcomeScreen

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

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        bottomBar = {
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
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
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

            // Auth Flow
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                )
            }
            
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Onboarding Carousel
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinishOnboarding = {
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
                    onNavigateToCultural = { navController.navigate(Screen.CulturalContext.route) },
                    onNavigateToFlashcards = { navController.navigate(Screen.FlashcardDeck.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) }
                )
            }

            // Profile
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
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

            // Practice (Games Hub & Mini-Games)
            composable(Screen.GameHub.route) {
                GameHubScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToWordMatch = { navController.navigate(Screen.WordMatchGame.route) },
                    onNavigateToFillBlank = { navController.navigate(Screen.FillBlankGame.route) },
                    onNavigateToAudioQuiz = { navController.navigate(Screen.AudioQuizGame.route) },
                    onNavigateToAspectBuilder = { navController.navigate(Screen.AspectBuilderGame.route) },
                    onNavigateToSentenceOrder = { navController.navigate(Screen.SentenceOrderGame.route) }
                )
            }
            composable(Screen.WordMatchGame.route) {
                WordMatchGameScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.FillBlankGame.route) {
                FillBlankGameScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.AudioQuizGame.route) {
                AudioQuizGameScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.AspectBuilderGame.route) {
                AspectBuilderGameScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.SentenceOrderGame.route) {
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
            composable(Screen.About.route) {
                AboutScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
