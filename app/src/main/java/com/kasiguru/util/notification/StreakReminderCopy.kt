package com.kasiguru.util.notification

import com.kasiguru.util.pluralize

/**
 * What the evening reminder says.
 *
 * It names what is actually left of today's streak quota: the due review, if any, and the games
 * still to play. The old body said "A short lesson keeps the streak going", which was not true;
 * a lesson does not count toward the streak, and following the advice did not save it.
 *
 * Pure text, kept out of the notification builder so the wording can be tested without Android.
 */
object StreakReminderCopy {

    fun title(currentStreak: Int): String =
        if (currentStreak > 0) "Keep your $currentStreak-day streak alive 🔥" else "Learn Kasiguranin today 📚"

    /**
     * @param dueCount words scheduled for review today; with none there is no review to ask for.
     * @param reviewDone whether today's review is already finished.
     * @param gamesLeft mini-game levels still to play today.
     */
    fun body(currentStreak: Int, dueCount: Int, reviewDone: Boolean, gamesLeft: Int, requiredGames: Int = 3): String {
        val review = if (dueCount > 0 && !reviewDone) "review ${pluralize(dueCount, "word")}" else null
        val games = when {
            gamesLeft <= 0 -> null
            gamesLeft >= requiredGames -> "play ${pluralize(gamesLeft, "game")}"
            else -> "play $gamesLeft more ${if (gamesLeft == 1) "game" else "games"}"
        }
        val goal = if (currentStreak > 0) "to keep your streak" else "to start a streak"
        val todo = listOfNotNull(review, games).joinToString(" and ")
        return if (todo.isEmpty()) "Today's streak is safe." else "${todo.replaceFirstChar { it.uppercase() }} $goal."
    }
}
