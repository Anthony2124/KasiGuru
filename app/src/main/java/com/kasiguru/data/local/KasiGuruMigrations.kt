package com.kasiguru.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migrations for KasiGuruDatabase v1 -> v21.
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
        MIGRATION_18_19,
        MIGRATION_19_20,
        MIGRATION_20_21,
        MIGRATION_21_22,
        MIGRATION_22_23,
        MIGRATION_23_24,
        MIGRATION_24_25,
        MIGRATION_25_26,
        MIGRATION_26_27,
        MIGRATION_27_28,
        MIGRATION_28_29
        )
    }

    // -- v28 -> v29 -----------------------------------------------------------
    // user_progress gains daily streak quota tracking fields:
    // dailyReviewCompletedDate, dailyGamesDate, and dailyGamesPlayedCount.
    // Syncs streak requirements with cloud progress and restores seamlessly upon sign-in.
    private val MIGRATION_28_29 = object : Migration(28, 29) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `user_progress` ADD COLUMN `dailyReviewCompletedDate` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `user_progress` ADD COLUMN `dailyGamesDate` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `user_progress` ADD COLUMN `dailyGamesPlayedCount` INTEGER NOT NULL DEFAULT 0")
        }
    }

    // -- v27 -> v28 -----------------------------------------------------------
    // vocabulary gains a part of speech and a two-language definition.
    //
    // partOfSpeech closes a loop that was open for several versions: the admin portal, the admin
    // word table and the in-app contribution form all wrote it to Firestore, and the device had no
    // column to receive it, so it was collected everywhere and readable nowhere.
    //
    // meaningEnglish/meaningTagalog are new content. Existing rows get empty strings and every
    // consumer treats an empty meaning as "not written yet" rather than as an error, exactly as
    // exampleSentence already behaves - so there is no backfill to do at migration time. The
    // definitions ship in DatabaseSeeder for a fresh install and reach existing installs through
    // the normal content sync once functions/backfill_meanings.js has run.
    private val MIGRATION_27_28 = object : Migration(27, 28) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `vocabulary` ADD COLUMN `partOfSpeech` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `vocabulary` ADD COLUMN `meaningEnglish` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `vocabulary` ADD COLUMN `meaningTagalog` TEXT NOT NULL DEFAULT ''")
        }
    }

    // -- v26 -> v27 -----------------------------------------------------------
    // vocabulary gains the two fields the memory model needs: how many times a known word has been
    // forgotten again (lapses), and where it currently sits on the relearning ladder. Both ALTER
    // with a default so existing rows join the new scheme as never-lapsed, normally-scheduled words,
    // which is what they were before this shipped.
    private val MIGRATION_26_27 = object : Migration(26, 27) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `vocabulary` ADD COLUMN `lapses` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `vocabulary` ADD COLUMN `relearningStep` INTEGER NOT NULL DEFAULT 0")
        }
    }

    // -- v25 -> v26 -----------------------------------------------------------
    // New table: profiles - the identity/roster layer for shared-device multi-profile support.
    // See ProfileEntity's doc comment for what this pass does and deliberately does not do yet
    // (progress itself is still one shared row per device, not per profile).
    private val MIGRATION_25_26 = object : Migration(25, 26) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `profiles` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, `residentName` TEXT NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, `isActive` INTEGER NOT NULL)"
            )
        }
    }

    // -- v24 -> v25 -----------------------------------------------------------
    // vocabulary gains a second example sentence per meaning (kasiguranin + translation),
    // enforcing "two sentences per meaning" going forward. Existing entries get empty strings
    // for the new columns, same as exampleSentence/exampleTranslation already default to when
    // unset - every consumer already treats an empty example as "none yet," not an error, so
    // there is no migration-time backfill to do here; authoring the missing content is a corpus
    // task tracked separately; this pass is schema only.
    private val MIGRATION_24_25 = object : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `vocabulary` ADD COLUMN `exampleSentence2` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `vocabulary` ADD COLUMN `exampleTranslation2` TEXT NOT NULL DEFAULT ''")
        }
    }

    // -- v23 -> v24 -----------------------------------------------------------
    // user_progress gains submissionsMade, backing the new "First Contribution" badge.
    private val MIGRATION_23_24 = object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `user_progress` ADD COLUMN `submissionsMade` INTEGER NOT NULL DEFAULT 0")
        }
    }

    // -- v22 -> v23 -----------------------------------------------------------
    // Badge redesign: achievements gains metricType (unlock logic reads this instead of five
    // hardcoded per-badge-family functions in UserProgressRepository) and tier (wires the
    // already-existing TierGold/Silver/Bronze theme tokens to real badges).
    //
    // Safe with respect to progress: this only adds columns and backfills them on the eleven
    // existing rows by id, matching MetricType's constants and UserProgressRepository's old
    // per-family thresholds exactly. isUnlocked/unlockedDate/currentValue/xpReward on every row
    // are untouched, so nobody's already-earned badges move. Not reseeded-from-empty like
    // MIGRATION_21_22 used for stories, because that would discard real per-user unlock state -
    // a badge is not interchangeable content the way a folk tale is.
    //
    // New achievement rows (submission-based, deeper streak/mastery, social/leaderboard) are
    // added by DatabaseSeeder, guarded the normal way (only on an empty table) for a fresh
    // install; an upgrading install picks them up via UserProgressRepository.seedNewAchievements,
    // called once from AchievementsViewModel's init, which inserts only ids not already present.
    private val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `achievements` ADD COLUMN `metricType` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `achievements` ADD COLUMN `tier` TEXT")

            fun backfill(id: String, metricType: String) {
                db.execSQL(
                    "UPDATE `achievements` SET `metricType` = ? WHERE `id` = ?",
                    arrayOf(metricType, id)
                )
            }
            backfill("first_word", "wordsLearned")
            backfill("ten_words", "wordsLearned")
            backfill("fifty_words", "wordsLearned")
            backfill("first_story", "storiesCompleted")
            backfill("all_stories", "storiesCompleted")
            backfill("first_game", "gamesPlayed")
            backfill("perfect_game", "perfectGame")
            backfill("three_day_streak", "streak")
            backfill("seven_day_streak", "streak")
            backfill("level_five", "level")
            backfill("level_ten", "level")
        }
    }

    /**
     * Clears the locally seeded placeholder leaderboard rows ("Ligaya Santos" and
     * friends). Rankings now come from the server-maintained leaderboard, and this
     * table is only an offline cache of it. Touches no user progress.
     */
    /**
     * Adds `lesson_progress`, the store behind the new lesson system.
     *
     * Only the learner's progress is persisted; lesson identity is derived from the vocabulary
     * categories at runtime, so the corpus can grow through the admin portal without another
     * migration. Create statement matches the entity exactly, including the index, because Room
     * validates schema identity on open.
     */
    // -- v21 -> v22 -----------------------------------------------------------
    // The story corpus was replaced: the five Casiguran tales gave way to ten Tagalog folk stories.
    // No schema change - this exists purely to make existing installs pick the new set up.
    //
    // Changing DatabaseSeeder alone would not have done it. Every seeding path is guarded by
    // `getStoryCount() == 0`, so an install that already has stories keeps the old five forever.
    // Emptying the table hands the work back to those guards, which reseed on the next read.
    //
    // Safe with respect to progress: reading state lives on the story rows themselves, and the
    // replacement corpus is a different set of stories, so there is no per-story progress to carry
    // across. Aggregate counters on user_progress (storiesCompleted, XP already awarded) are in a
    // different table and are deliberately left untouched - a learner does not un-earn XP.
    private val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DELETE FROM `stories`")
        }
    }

    private val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `lesson_progress` (" +
                    "`unitId` TEXT NOT NULL, `lessonIndex` INTEGER NOT NULL, " +
                    "`isComplete` INTEGER NOT NULL, `bestAccuracy` REAL NOT NULL, " +
                    "`timesCompleted` INTEGER NOT NULL, `lastCompletedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`unitId`, `lessonIndex`))"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_lesson_progress_isComplete` " +
                    "ON `lesson_progress` (`isComplete`)"
            )
            // Daily-XP ledger, so the daily-goal ring stops being derived from a modulo of total XP.
            db.execSQL("ALTER TABLE `user_progress` ADD COLUMN `dailyXpEarned` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `user_progress` ADD COLUMN `dailyXpDate` TEXT NOT NULL DEFAULT ''")
        }
    }

    private val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DELETE FROM `leaderboard`")
        }
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
