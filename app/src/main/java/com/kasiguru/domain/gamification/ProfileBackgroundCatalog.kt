package com.kasiguru.domain.gamification

/** Stable IDs travel through Room, private sync and the public profile. */
data class ProfileBackground(val id: String, val title: String, val level: Int = 1, val streak: Int = 0) {
    fun isUnlocked(accountLevel: Int, longestStreak: Int): Boolean = accountLevel >= level || (streak > 0 && longestStreak >= streak)
    val requirement: String get() = when {
        level == 1 -> "Free"
        level == Int.MAX_VALUE -> "$streak-day streak"
        streak > 0 -> "Level $level or $streak-day streak"
        else -> "Level $level"
    }
}
object ProfileBackgroundCatalog {
    const val DEFAULT = "forest"
    val backgrounds = listOf(
        ProfileBackground("forest", "Forest"),
        ProfileBackground("casapsapan", "Casapsapan Beach"),
        ProfileBackground("farm", "Casiguran Farm", level = 2),
        ProfileBackground("river", "River", level = 3),
        ProfileBackground("ermita_hill", "Ermita Hill", level = 4),
        ProfileBackground("ontok_lighthouse", "Ontok Lighthouse", level = 5, streak = 7),
        ProfileBackground("tibu_tidal_pool", "Tibu Tidal Pool", level = Int.MAX_VALUE, streak = 30)
    )
    fun find(id: String): ProfileBackground? = backgrounds.firstOrNull { it.id == id }
    fun normalize(id: String): String = find(id)?.id ?: DEFAULT
}
