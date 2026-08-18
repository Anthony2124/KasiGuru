package com.kasiguru.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the full v1 -> v21 migration chain against the exported schemas.
 * Run with: ./gradlew connectedDebugAndroidTest
 * (instrumented tests require an emulator or device)
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        KasiGuruDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrateAllVersionsFromEmpty() {
        // Start from an empty v1 database and validate every step to v17.
        helper.createDatabase(testDbName, 1).close()
        helper.runMigrationsAndValidate(testDbName, 21, true, *KasiGuruMigrations.ALL)
    }

    @Test
    fun migrateWithExistingDataPreservesRows() {
        // Create a v1 DB, seed a vocabulary row, then migrate to v17 and
        // confirm the row (and its SRS defaults) survived.
        helper.createDatabase(testDbName, 1).apply {
            execSQL(
                "INSERT INTO vocabulary " +
                    "(id, kasiguranin, tagalog, english, rootForm, neutralForm, " +
                    " imperfectiveForm, perfectiveForm, contemplativeForm, category, " +
                    " audioFileName, exampleSentence, exampleTranslation, isLearned, timesReviewed) " +
                    "VALUES (1, 'abben', 'taga', 'to help', 'abben', '', '', '', '', 'Body Parts & Health', '', '', '', 0, 0)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 19, true, *KasiGuruMigrations.ALL)
        db.query("SELECT kasiguranin, easinessFactor, intervalDays FROM vocabulary WHERE id = 1").use { cursor ->
            check(cursor.moveToFirst()) { "seeded row was lost during migration" }
            check(cursor.getString(0) == "abben") { "kasiguranin mismatch" }
            check(cursor.getDouble(1) == 2.5) { "easinessFactor default mismatch" }
            check(cursor.getInt(2) == 0) { "intervalDays default mismatch" }
        }
        db.close()
    }

    @Test
    fun migrateV20ToV21AddsLessonProgressAndDailyXpLedger() {
        // v20 -> v21 was previously untested by this suite despite adding a whole table
        // and two new columns. Start from a real v20 row and confirm it survives with
        // the new columns correctly defaulted, and that the new table is actually there.
        helper.createDatabase(testDbName, 20).apply {
            execSQL(
                "INSERT INTO user_progress " +
                    "(id, userName, password, email, fullName, age, address, profileIconId, " +
                    " totalXp, level, currentStreak, longestStreak, lastActiveDate, wordsLearned, " +
                    " storiesCompleted, gamesPlayed, totalCorrectAnswers, totalQuestionsAnswered, " +
                    " lessonsCompleted, isOnboardingCompleted, dailyGoalXp, titleBadge, updatedAt) " +
                    "VALUES (1, 'Test', '', '', 'Test User', NULL, '', 1, 500, 3, 2, 5, " +
                    " '2026-01-01', 10, 1, 2, 20, 25, 3, 1, 50, 'Learner', 0)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 21, true, *KasiGuruMigrations.ALL)

        db.query("SELECT dailyXpEarned, dailyXpDate FROM user_progress WHERE id = 1").use { cursor ->
            check(cursor.moveToFirst()) { "seeded user_progress row was lost during migration" }
            check(cursor.getInt(0) == 0) { "dailyXpEarned default mismatch" }
            check(cursor.getString(1) == "") { "dailyXpDate default mismatch" }
        }

        // A schema-creation check, not a data-migration one — the table is brand new,
        // so "queryable at all" is what matters here.
        db.query("SELECT COUNT(*) FROM lesson_progress").use { cursor ->
            check(cursor.moveToFirst()) { "lesson_progress table missing or unqueryable" }
        }
        db.close()
    }
}
