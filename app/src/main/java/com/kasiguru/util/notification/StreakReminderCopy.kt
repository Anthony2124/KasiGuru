package com.kasiguru.util.notification

/**
 * What the nightly reminder says.
 *
 * The old body was the same sentence every night — "Practice for just 2 minutes tonight to master
 * vocabulary & verb aspects" — which named nothing the learner could act on and was not even true of
 * a night when nothing was scheduled. A reminder that says how many words are about to be forgotten
 * is both honest and specific, and the number is exactly the thing the schedule exists to surface.
 *
 * Pure text, kept out of the notification builder so the wording can be tested without Android.
 */
object StreakReminderCopy {

    fun title(currentStreak: Int): String =
        if (currentStreak > 0) "Keep your $currentStreak-day streak alive 🔥" else "Learn Kasiguranin today 📚"

    /**
     * Names the work waiting, or asks for a short lesson when there is none.
     *
     * The zero case matters: on a fresh install, and for a learner who is up to date, inventing a
     * review backlog would be a lie the app can be caught in one tap later.
     */
    fun body(dueCount: Int): String = when {
        dueCount <= 0 -> "A short lesson keeps the streak going."
        dueCount == 1 -> "1 word is due for review tonight."
        else -> "$dueCount words are due for review tonight."
    }
}
