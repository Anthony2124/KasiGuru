package com.kasiguru.domain.audio

import com.kasiguru.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicMoodTest {

    @Test
    fun `tab roots and menus play menu music`() {
        listOf(Screen.Learn, Screen.Library, Screen.Profile, Screen.Settings, Screen.Leaderboard,
            Screen.VocabularyList, Screen.VocabularyDetail, Screen.Achievements, Screen.Streak)
            .forEach { assertEquals(it.route, MusicMood.Menu, musicMoodFor(it.route)) }
    }

    @Test
    fun `every game route plays game music`() {
        listOf(Screen.GameHub, Screen.LevelSelection, Screen.WordMatchGame, Screen.ReverseMatchGame,
            Screen.FillBlankGame, Screen.RecallGame, Screen.AspectBuilderGame, Screen.SentenceOrderGame,
            Screen.WordWheelGame, Screen.WordSearchCategories, Screen.WordSearchGame)
            .forEach { assertEquals(it.route, MusicMood.Game, musicMoodFor(it.route)) }
    }

    @Test
    fun `listening screens and first run are silent`() {
        listOf(Screen.LessonPlayer, Screen.StoryList, Screen.StoryReader, Screen.FlashcardDeck,
            Screen.Splash, Screen.Onboarding, Screen.AccountSuspended)
            .forEach { assertEquals(it.route, MusicMood.None, musicMoodFor(it.route)) }
        assertEquals(MusicMood.None, musicMoodFor(null))
    }
}
