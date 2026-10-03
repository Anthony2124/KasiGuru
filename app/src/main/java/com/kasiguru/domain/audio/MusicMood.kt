package com.kasiguru.domain.audio

/** Which background loop a screen plays, if any. */
enum class MusicMood { Menu, Game, None }

/**
 * Maps a navigation route pattern to its music. Lessons, stories and flashcards stay silent
 * because a learner is listening to pronunciation recordings there; first-run screens stay
 * silent so the app does not open with music before the learner has seen the settings.
 */
fun musicMoodFor(route: String?): MusicMood = when {
    route == null -> MusicMood.None
    route == "games" || route.startsWith("games/") -> MusicMood.Game
    route.startsWith("lesson/") -> MusicMood.None
    route == "stories" || route.startsWith("story/") -> MusicMood.None
    route == "flashcards" -> MusicMood.None
    route in silentRoutes -> MusicMood.None
    else -> MusicMood.Menu
}

private val silentRoutes = setOf("splash", "onboarding", "account_suspended")
