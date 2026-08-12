package com.kasiguru.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migrations for KasiGuruDatabase v1 -> v17.
 *
 * Generated from the exported schemas in app/schemas/com.kasiguru.data.local.KasiGuruDatabase/
 * so every create statement matches the target schema byte-for-byte (Room validates
 * the resulting schema identity, including column types, nullability, defaults,
 * primary keys, and indexes).
 *
 * Tables that only gain columns are migrated with the safe four-step recipe
 * (rename -> create -> copy -> drop) instead of ALTER TABLE ADD COLUMN, because
 * several added columns are NOT NULL without defaults, which SQLite cannot add
 * to non-empty tables.
 */
object KasiGuruMigrations {

    /**
     * The complete, ordered migration chain. Pass to
     * Room.databaseBuilder(...).addMigrations(*KasiGuruMigrations.ALL).
     */
    val ALL: Array<Migration> by lazy {
        arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11,
        MIGRATION_11_12,
        MIGRATION_12_13,
        MIGRATION_13_14,
        MIGRATION_14_15,
        MIGRATION_15_16,
        MIGRATION_16_17,
        MIGRATION_17_18,
        MIGRATION_18_19
        )
    }

    private fun recreateTable(
        db: SupportSQLiteDatabase,
        tableName: String,
        createSql: String,
        insertColumns: String,
        selectSql: String
    ) {
        val old = "${tableName}_old"
        db.execSQL("ALTER TABLE `$tableName` RENAME TO `$old`")
        db.execSQL(createSql)
        db.execSQL("INSERT INTO `$tableName` ($insertColumns) SELECT $selectSql FROM `$old`")
        db.execSQL("DROP TABLE `$old`")
    }

    // -- v1 -> v2 -------------------------------------------------------------
    // vocabulary gains: phoneticGlottal, phoneticVowelLength, ipaNotation (NOT NULL, no defaults)
    // new table: sync_queue
    private val VOCABULARY_V2_COLUMNS = listOf(
        "id", "kasiguranin", "tagalog", "english", "rootForm",
        "neutralForm", "imperfectiveForm", "perfectiveForm", "contemplativeForm",
        "category", "audioFileName", "exampleSentence", "exampleTranslation",
        "phoneticGlottal", "phoneticVowelLength", "ipaNotation",
        "isLearned", "timesReviewed"
    )

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            recreateTable(
                db,
                "vocabulary",
                "CREATE TABLE IF NOT EXISTS `vocabulary` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`kasiguranin` TEXT NOT NULL, `tagalog` TEXT NOT NULL, `english` TEXT NOT NULL, " +
                    "`rootForm` TEXT NOT NULL, `neutralForm` TEXT NOT NULL, " +
                    "`imperfectiveForm` TEXT NOT NULL, `perfectiveForm` TEXT NOT NULL, " +
                    "`contemplativeForm` TEXT NOT NULL, `category` TEXT NOT NULL, " +
                    "`audioFileName` TEXT NOT NULL, `exampleSentence` TEXT NOT NULL, " +
                    "`exampleTranslation` TEXT NOT NULL, " +
                    "`phoneticGlottal` INTEGER NOT NULL, `phoneticVowelLength` INTEGER NOT NULL, " +
                    "`ipaNotation` TEXT NOT NULL, `isLearned` INTEGER NOT NULL, `timesReviewed` INTEGER NOT NULL)",
                VOCABULARY_V2_COLUMNS.joinToString(),
                "id, kasiguranin, tagalog, english, rootForm, neutralForm, " +
                    "imperfectiveForm, perfectiveForm, contemplativeForm, category, " +
                    "audioFileName, exampleSentence, exampleTranslation, 0, 0, '', " +
                    "isLearned, timesReviewed"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `sync_queue` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`payloadType` TEXT NOT NULL, `payloadRef` TEXT NOT NULL, " +
                    "`payloadData` TEXT NOT NULL, `status` TEXT NOT NULL, `queuedAt` INTEGER NOT NULL)"
            )
        }
    }

    // -- v2 -> v3 (no schema change) ------------------------------------------
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) = Unit
    }

    // -- v3 -> v4 -------------------------------------------------------------
    // user_progress gains: password, email (NOT NULL, no defaults)
    private val USER_PROGRESS_V4_COLUMNS = listOf(
        "id", "userName", "password", "email", "totalXp", "level",
        "currentStreak", "longestStreak", "lastActiveDate", "wordsLearned",
        "storiesCompleted", "gamesPlayed", "totalCorrectAnswers",
        "totalQuestionsAnswered", "lessonsCompleted"
    )

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            recreateTable(
                db,
                "user_progress",
                "CREATE TABLE IF NOT EXISTS `user_progress` (" +
                    "`id` INTEGER NOT NULL, `userName` TEXT NOT NULL, `password` TEXT NOT NULL, " +
                    "`email` TEXT NOT NULL, `totalXp` INTEGER NOT NULL, `level` INTEGER NOT NULL, " +
                    "`currentStreak` INTEGER NOT NULL, `longestStreak` INTEGER NOT NULL, " +
                    "`lastActiveDate` TEXT NOT NULL, `wordsLearned` INTEGER NOT NULL, " +
                    "`storiesCompleted` INTEGER NOT NULL, `gamesPlayed` INTEGER NOT NULL, " +
                    "`totalCorrectAnswers` INTEGER NOT NULL, `totalQuestionsAnswered` INTEGER NOT NULL, " +
                    "`lessonsCompleted` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                USER_PROGRESS_V4_COLUMNS.joinToString(),
                "id, userName, '', '', totalXp, level, currentStreak, longestStreak, " +
                    "lastActiveDate, wordsLearned, storiesCompleted, gamesPlayed, " +
                    "totalCorrectAnswers, totalQuestionsAnswered, lessonsCompleted"
            )
        }
    }

    // -- v4 -> v5 -------------------------------------------------------------
    // user_progress gains: fullName, age, address, profileIconId (no defaults)
    private val USER_PROGRESS_V5_COLUMNS = USER_PROGRESS_V4_COLUMNS + listOf(
        "fullName", "age", "address", "profileIconId"
    )

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            recreateTable(
                db,
                "user_progress",
                "CREATE TABLE IF NOT EXISTS `user_progress` (" +
                    "`id` INTEGER NOT NULL, `userName` TEXT NOT NULL, `password` TEXT NOT NULL, " +
                    "`email` TEXT NOT NULL, `fullName` TEXT NOT NULL, `age` INTEGER, " +
                    "`address` TEXT NOT NULL, `profileIconId` INTEGER NOT NULL, " +
                    "`totalXp` INTEGER NOT NULL, `level` INTEGER NOT NULL, " +
                    "`currentStreak` INTEGER NOT NULL, `longestStreak` INTEGER NOT NULL, " +
                    "`lastActiveDate` TEXT NOT NULL, `wordsLearned` INTEGER NOT NULL, " +
                    "`storiesCompleted` INTEGER NOT NULL, `gamesPlayed` INTEGER NOT NULL, " +
                    "`totalCorrectAnswers` INTEGER NOT NULL, `totalQuestionsAnswered` INTEGER NOT NULL, " +
                    "`lessonsCompleted` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                USER_PROGRESS_V5_COLUMNS.joinToString(),
                "id, userName, password, email, '', NULL, '', 1, totalXp, level, " +
                    "currentStreak, longestStreak, lastActiveDate, wordsLearned, " +
                    "storiesCompleted, gamesPlayed, totalCorrectAnswers, " +
                    "totalQuestionsAnswered, lessonsCompleted"
            )
        }
    }

    // -- v5 -> v6 -------------------------------------------------------------
    // vocabulary gains SRS fields: easinessFactor, intervalDays, nextReviewDate (no defaults)
    private val VOCABULARY_V6_COLUMNS = VOCABULARY_V2_COLUMNS + listOf(
        "easinessFactor", "intervalDays", "nextReviewDate"
    )

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            recreateTable(
                db,
                "vocabulary",
                "CREATE TABLE IF NOT EXISTS `vocabulary` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`kasiguranin` TEXT NOT NULL, `tagalog` TEXT NOT NULL, `english` TEXT NOT NULL, " +
                    "`rootForm` TEXT NOT NULL, `neutralForm` TEXT NOT NULL, " +
                    "`imperfectiveForm` TEXT NOT NULL, `perfectiveForm` TEXT NOT NULL, " +
                    "`contemplativeForm` TEXT NOT NULL, `category` TEXT NOT NULL, " +
                    "`audioFileName` TEXT NOT NULL, `exampleSentence` TEXT NOT NULL, " +
                    "`exampleTranslation` TEXT NOT NULL, " +
                    "`phoneticGlottal` INTEGER NOT NULL, `phoneticVowelLength` INTEGER NOT NULL, " +
                    "`ipaNotation` TEXT NOT NULL, `isLearned` INTEGER NOT NULL, `timesReviewed` INTEGER NOT NULL, " +
                    "`easinessFactor` REAL NOT NULL, `intervalDays` INTEGER NOT NULL, " +
                    "`nextReviewDate` TEXT NOT NULL)",
                VOCABULARY_V6_COLUMNS.joinToString(),
                "id, kasiguranin, tagalog, english, rootForm, neutralForm, " +
                    "imperfectiveForm, perfectiveForm, contemplativeForm, category, " +
                    "audioFileName, exampleSentence, exampleTranslation, " +
                    "phoneticGlottal, phoneticVowelLength, ipaNotation, isLearned, timesReviewed, " +
                    "2.5, 0, ''"
            )
        }
    }

    // -- v6 -> v7, v7 -> v8, v8 -> v9 (no schema changes) ---------------------
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) = Unit
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) = Unit
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) = Unit
    }

    // -- v9 -> v10 ------------------------------------------------------------
    // new table: leaderboard
    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `leaderboard` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, `totalXp` INTEGER NOT NULL, " +
                    "`currentStreak` INTEGER NOT NULL, `avatarIconId` INTEGER NOT NULL, " +
                    "`levelTitle` TEXT NOT NULL, `isCurrentUser` INTEGER NOT NULL)"
            )
        }
    }

    // -- v10 -> v11 -----------------------------------------------------------
    // new table: notifications
    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `notifications` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`title` TEXT NOT NULL, `message` TEXT NOT NULL, `timestamp` TEXT NOT NULL, " +
                    "`category` TEXT NOT NULL, `isRead` INTEGER NOT NULL, `deepLinkRoute` TEXT NOT NULL)"
            )
        }
    }

    // -- v11 -> v12 -----------------------------------------------------------
    // vocabulary: audioFileName -> audioResName (rename) + defaults introduced
    private val VOCABULARY_V12_COLUMNS = listOf(
        "id", "kasiguranin", "tagalog", "english", "rootForm",
        "neutralForm", "imperfectiveForm", "perfectiveForm", "contemplativeForm",
        "category", "audioResName", "exampleSentence", "exampleTranslation",
        "phoneticGlottal", "phoneticVowelLength", "ipaNotation",
        "isLearned", "timesReviewed", "easinessFactor", "intervalDays", "nextReviewDate"
    )

    private val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            recreateTable(
                db,
                "vocabulary",
                "CREATE TABLE IF NOT EXISTS `vocabulary` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`kasiguranin` TEXT NOT NULL, `tagalog` TEXT NOT NULL, `english` TEXT NOT NULL, " +
                    "`rootForm` TEXT NOT NULL, `neutralForm` TEXT NOT NULL DEFAULT '', " +
                    "`imperfectiveForm` TEXT NOT NULL DEFAULT '', `perfectiveForm` TEXT NOT NULL DEFAULT '', " +
                    "`contemplativeForm` TEXT NOT NULL DEFAULT '', `category` TEXT NOT NULL, " +
                    "`audioResName` TEXT NOT NULL DEFAULT '', `exampleSentence` TEXT NOT NULL DEFAULT '', " +
                    "`exampleTranslation` TEXT NOT NULL DEFAULT '', " +
                    "`phoneticGlottal` INTEGER NOT NULL DEFAULT 0, " +
                    "`phoneticVowelLength` INTEGER NOT NULL DEFAULT 0, " +
                    "`ipaNotation` TEXT NOT NULL DEFAULT '', `isLearned` INTEGER NOT NULL DEFAULT 0, " +
                    "`timesReviewed` INTEGER NOT NULL, `easinessFactor` REAL NOT NULL, " +
                    "`intervalDays` INTEGER NOT NULL, `nextReviewDate` TEXT NOT NULL)",
                VOCABULARY_V12_COLUMNS.joinToString(),
                "id, kasiguranin, tagalog, english, rootForm, neutralForm, " +
                    "imperfectiveForm, perfectiveForm, contemplativeForm, category, " +
                    "audioFileName, exampleSentence, exampleTranslation, " +
                    "phoneticGlottal, phoneticVowelLength, ipaNotation, isLearned, timesReviewed, " +
                    "easinessFactor, intervalDays, nextReviewDate"
            )
        }
    }

    // -- v12 -> v13 (no schema change) ----------------------------------------
    private val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) = Unit
    }

    // -- v13 -> v14 -----------------------------------------------------------
    // user_progress gains: isOnboardingCompleted, dailyGoalXp, titleBadge (no defaults)
    private val USER_PROGRESS_V14_COLUMNS = USER_PROGRESS_V5_COLUMNS + listOf(
        "isOnboardingCompleted", "dailyGoalXp", "titleBadge"
    )

    private val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            recreateTable(
                db,
                "user_progress",
                "CREATE TABLE IF NOT EXISTS `user_progress` (" +
                    "`id` INTEGER NOT NULL, `userName` TEXT NOT NULL, `password` TEXT NOT NULL, " +
                    "`email` TEXT NOT NULL, `fullName` TEXT NOT NULL, `age` INTEGER, " +
                    "`address` TEXT NOT NULL, `profileIconId` INTEGER NOT NULL, " +
                    "`totalXp` INTEGER NOT NULL, `level` INTEGER NOT NULL, " +
                    "`currentStreak` INTEGER NOT NULL, `longestStreak` INTEGER NOT NULL, " +
                    "`lastActiveDate` TEXT NOT NULL, `wordsLearned` INTEGER NOT NULL, " +
                    "`storiesCompleted` INTEGER NOT NULL, `gamesPlayed` INTEGER NOT NULL, " +
                    "`totalCorrectAnswers` INTEGER NOT NULL, `totalQuestionsAnswered` INTEGER NOT NULL, " +
                    "`lessonsCompleted` INTEGER NOT NULL, " +
                    "`isOnboardingCompleted` INTEGER NOT NULL, `dailyGoalXp` INTEGER NOT NULL, " +
                    "`titleBadge` TEXT NOT NULL, PRIMARY KEY(`id`))",
                USER_PROGRESS_V14_COLUMNS.joinToString(),
                "id, userName, password, email, fullName, age, address, profileIconId, " +
                    "totalXp, level, currentStreak, longestStreak, lastActiveDate, wordsLearned, " +
                    "storiesCompleted, gamesPlayed, totalCorrectAnswers, " +
                    "totalQuestionsAnswered, lessonsCompleted, 0, 100, ''"
            )
        }
    }

    // -- v14 -> v15 (no schema change) ----------------------------------------
    private val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) = Unit
    }

    // -- v15 -> v16 -----------------------------------------------------------
    // new table: game_levels
    private val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `game_levels` (" +
                    "`gameType` TEXT NOT NULL, `levelNumber` INTEGER NOT NULL, " +
                    "`difficulty` TEXT NOT NULL, `starsEarned` INTEGER NOT NULL, " +
                    "`isUnlocked` INTEGER NOT NULL, `questionsCount` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`gameType`, `levelNumber`))"
            )
        }
    }

    // -- v16 -> v17 -----------------------------------------------------------
    // new table: conjugations (FK -> vocabulary, index on vocabulary_id)
    private val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `conjugations` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`vocabulary_id` INTEGER NOT NULL, `conjugated_form` TEXT NOT NULL, " +
                    "`tense` TEXT NOT NULL, `affix_type` TEXT, " +
                    "FOREIGN KEY(`vocabulary_id`) REFERENCES `vocabulary`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_conjugations_vocabulary_id` " +
                    "ON `conjugations` (`vocabulary_id`)"
            )
        }
    }

    // -- v17 -> v18 -----------------------------------------------------------
    // Performance indexes on hot query columns (Phase 3). Index-only change:
    // no columns added/removed, so the migration just creates the indexes.
    private val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_vocabulary_category` ON `vocabulary` (`category`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_vocabulary_isLearned` ON `vocabulary` (`isLearned`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_vocabulary_nextReviewDate` ON `vocabulary` (`nextReviewDate`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_vocabulary_timesReviewed` ON `vocabulary` (`timesReviewed`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_stories_requiredXp` ON `stories` (`requiredXp`)")
        }
    }

    // -- v18 -> v19 -----------------------------------------------------------
    // user_progress gains: updatedAt (cross-device sync marker).
    // ALTER with a default so existing rows are populated.
    private val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `user_progress` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
        }
    }
}
