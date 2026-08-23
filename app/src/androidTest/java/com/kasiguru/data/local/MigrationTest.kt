package com.kasiguru.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the full v1 -> v26 migration chain against the exported schemas.
 * Run with: ./gradlew connectedDebugAndroidTest
 * (instrumented tests require an emulator or device)
 *
 * These tests previously stopped at v21 while the database was already at v26, leaving
 * MIGRATION_21_22 through MIGRATION_25_26 — five migrations that run against real user
 * data on upgrade — with no coverage at all.
 *
 * Note on v23: `app/schemas/.../23.json` was never exported (the chain jumps 22 -> 24 on
 * disk). It cannot be regenerated faithfully now, because the entity definitions have moved
 * on through v24, v25 and v26 — recompiling at version 23 today would emit v26's shape under
 * a v23 filename, which is worse than having no file. This is tolerable because
 * MigrationTestHelper only reads the schema for the version it *creates* at and the version
 * it *validates* against; the intermediate migrations run as plain SQL. So the full 1 -> 26
 * sweep below genuinely executes MIGRATION_22_23 and MIGRATION_23_24 even though neither
 * endpoint can be pinned at 23. What is lost is only the ability to start or stop a test
 * exactly at v23. Do not let this recur: a version bump must ship with its schema JSON.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private companion object {
        /**
         * Must track [KasiGuruDatabase]'s `version`. Kept here so a version bump that
         * forgets to extend this suite fails loudly against the missing schema export
         * rather than quietly continuing to test an old ceiling.
         */
        const val CURRENT_VERSION = 27
    }

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
        // Start from an empty v1 database and validate the whole chain against the
        // current schema version.
        helper.createDatabase(testDbName, 1).close()
        helper.runMigrationsAndValidate(testDbName, CURRENT_VERSION, true, *KasiGuruMigrations.ALL)
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

        val db = helper.runMigrationsAndValidate(testDbName, CURRENT_VERSION, true, *KasiGuruMigrations.ALL)
        db.query(
            "SELECT kasiguranin, easinessFactor, intervalDays, exampleSentence2, exampleTranslation2 " +
                "FROM vocabulary WHERE id = 1"
        ).use { cursor ->
            check(cursor.moveToFirst()) { "seeded row was lost during migration" }
            check(cursor.getString(0) == "abben") { "kasiguranin mismatch" }
            check(cursor.getDouble(1) == 2.5) { "easinessFactor default mismatch" }
            check(cursor.getInt(2) == 0) { "intervalDays default mismatch" }
            // Added by MIGRATION_24_25; existing rows are meant to default to empty, not null.
            check(cursor.getString(3) == "") { "exampleSentence2 default mismatch" }
            check(cursor.getString(4) == "") { "exampleTranslation2 default mismatch" }
        }
        db.close()
    }

    /**
     * The v24 -> v26 tail: the two example-sentence columns and the `profiles` table that
     * multi-profile support rests on. Starts at 24 rather than 23 for the reason in the
     * class comment — 23.json was never exported.
     */
    @Test
    fun migrateV24ToCurrentAddsExampleColumnsAndProfilesTable() {
        helper.createDatabase(testDbName, 24).apply {
            execSQL(
                "INSERT INTO user_progress " +
                    "(id, userName, password, email, fullName, age, address, profileIconId, " +
                    " totalXp, level, currentStreak, longestStreak, lastActiveDate, wordsLearned, " +
                    " storiesCompleted, gamesPlayed, totalCorrectAnswers, totalQuestionsAnswered, " +
                    " lessonsCompleted, isOnboardingCompleted, dailyGoalXp, dailyXpEarned, " +
                    " dailyXpDate, titleBadge, submissionsMade, updatedAt) " +
                    "VALUES (1, 'Test', '', '', 'Test User', NULL, '', 1, 500, 3, 2, 5, " +
                    " '2026-01-01', 10, 1, 2, 20, 25, 3, 1, 50, 0, '', 'Learner', 7, 0)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, CURRENT_VERSION, true, *KasiGuruMigrations.ALL)

        // submissionsMade arrived in v23 -> v24 and now syncs to Firestore; a non-default
        // value must survive the remaining migrations rather than being reset.
        db.query("SELECT submissionsMade FROM user_progress WHERE id = 1").use { cursor ->
            check(cursor.moveToFirst()) { "seeded user_progress row was lost during migration" }
            check(cursor.getInt(0) == 7) { "submissionsMade was not preserved" }
        }

        // Brand-new table in v25 -> v26, so "queryable at all" is the assertion that matters.
        db.query("SELECT id, name, residentName, createdAt, isActive FROM profiles").use { cursor ->
            check(cursor.count == 0) { "profiles should start empty" }
        }
        db.close()
    }

    /**
     * The v26 -> v27 tail: the memory-model columns.
     *
     * A learner upgrading into this version has a review history already, and it has to survive:
     * their words join the new scheme as never-lapsed and normally-scheduled, which is exactly what
     * they were before the columns existed.
     */
    @Test
    fun migrateV26ToV27AddsLapseTrackingWithoutDisturbingReviewHistory() {
        helper.createDatabase(testDbName, 26).apply {
            execSQL(
                "INSERT INTO vocabulary " +
                    "(id, kasiguranin, tagalog, english, rootForm, neutralForm, imperfectiveForm, " +
                    " perfectiveForm, contemplativeForm, category, audioResName, exampleSentence, " +
                    " exampleTranslation, exampleSentence2, exampleTranslation2, phoneticGlottal, " +
                    " phoneticVowelLength, ipaNotation, isLearned, timesReviewed, easinessFactor, " +
                    " intervalDays, nextReviewDate) " +
                    "VALUES (1, 'apak', 'daras', 'adze', 'apak', '', '', '', '', 'Occupations & Tools', " +
                    " '', '', '', '', '', 0, 0, '', 1, 9, 2.36, 30, '2026-09-20')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, CURRENT_VERSION, true, *KasiGuruMigrations.ALL)

        db.query(
            "SELECT lapses, relearningStep, timesReviewed, intervalDays, nextReviewDate, isLearned " +
                "FROM vocabulary WHERE id = 1"
        ).use { cursor ->
            check(cursor.moveToFirst()) { "seeded vocabulary row was lost during migration" }
            check(cursor.getInt(0) == 0) { "lapses default mismatch" }
            check(cursor.getInt(1) == 0) { "relearningStep default mismatch" }
            check(cursor.getInt(2) == 9) { "timesReviewed was not preserved" }
            check(cursor.getInt(3) == 30) { "intervalDays was not preserved" }
            check(cursor.getString(4) == "2026-09-20") { "nextReviewDate was not preserved" }
            check(cursor.getInt(5) == 1) { "isLearned was not preserved" }
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
