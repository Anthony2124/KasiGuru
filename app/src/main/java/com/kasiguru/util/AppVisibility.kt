package com.kasiguru.util

/**
 * Whether the app is on screen, as MainActivity's resume and pause say. A background check uses it
 * to show its news as the in-app pop-up rather than a phone notification while the learner is
 * already looking at the app.
 */
object AppVisibility {
    @Volatile
    var isForeground: Boolean = false
}
