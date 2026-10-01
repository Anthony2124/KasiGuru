package com.kasiguru.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kasiguru.data.local.entity.*
import com.kasiguru.data.repository.GamificationRepository
import com.kasiguru.domain.gamification.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class GamificationRepositoryTest {
    private lateinit var db: KasiGuruDatabase
    private lateinit var rewards: GamificationRepository
    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,
            KasiGuruDatabase::class.java).build()
        rewards = GamificationRepository(db)
    }
    @After fun close() { db.close() }
    @Test fun normalizationKeepsEvidenceAndLegacyBadgesWithoutGuessingMasteryOrPerfectGames() = runBlocking {
        db.userProgressDao().insertOrUpdate(UserProgressEntity(totalXp = 9999,level = 10,wordsLearned = 120,
            dailyXpDate = LocalDate.now().toString(),dailyXpEarned = 500,isOnboardingCompleted = true,dailyGoalXp = 150))
        db.lessonDao().upsert(LessonProgressEntity("theme:greetings",0,true,1f,4,1))
        db.gameLevelDao().insertAll(listOf(GameLevelEntity("word_match",1,"Easy",3,true)))
        db.storyDao().insert(DatabaseSeeder.getInitialStories().first().copy(isCompleted = true))
        db.achievementDao().insert(AchievementEntity(id = "first_word",name = "First Word",description = "Original badge",
            iconEmoji = "",category = "Progress",requiredValue = 1,isUnlocked = true,unlockedDate = "2025-01-01"))
        rewards.ensureNormalized()
        val p = db.userProgressDao().getUserProgressOnce()!!
        assertEquals(50,p.activityXp)
        assertEquals(80,p.badgeBonusXp)
        assertEquals(130,p.totalXp)
        assertEquals(2,p.level)
        assertEquals(0,p.dailyXpEarned)
        assertEquals(80,p.dailyGoalXp)
        assertEquals(9999,db.rewardDao().normalization()!!.originalXp)
        assertEquals("2025-01-01",db.achievementDao().getAchievementById("first_word")!!.unlockedDate)
        assertTrue(db.achievementDao().getAllAchievementsOnce().none { it.metricType == "verifiedWords" && it.isUnlocked })
        assertTrue(db.achievementDao().getAllAchievementsOnce().none { it.metricType == "perfectLevels" && it.isUnlocked })
        rewards.ensureNormalized()
        assertEquals(p.totalXp,db.userProgressDao().getUserProgressOnce()!!.totalXp)
    }
    @Test fun firstLessonAndBadgeCommitTogetherAndSameDayRepeatAwardsZero() = runBlocking {
        val first = rewards.lesson("theme:greetings",0,1f)
        assertEquals(25,first.activityXp)
        assertEquals(20,first.bonusXp)
        assertTrue(db.lessonDao().get("theme:greetings",0)!!.isComplete)
        assertNotNull(db.rewardDao().pendingCelebration().first())
        assertEquals(0,rewards.lesson("theme:greetings",0,1f).activityXp)
        assertEquals(0,rewards.lesson("theme:greetings",0,1f).bonusXp)
        assertEquals(1,db.userProgressDao().getUserProgressOnce()!!.lessonsCompleted)
    }
    @Test fun failedGamesEarnNothingAndDistinctPerfectLevelsCannotBeFarmed() = runBlocking {
        db.gameLevelDao().insertAll(listOf(GameLevelEntity("word_match",1,"Easy",isUnlocked = true),
            GameLevelEntity("word_match",2,"Easy")))
        assertEquals(0,rewards.game("word_match","word_match",1,0,5,0,false).activityXp)
        val first = rewards.game("word_match","word_match",1,5,5,3,true)
        assertEquals(20,first.activityXp)
        assertEquals(60,first.bonusXp)
        assertTrue(db.gameLevelDao().getLevel("word_match",2)!!.isUnlocked)
        assertEquals(0,rewards.game("word_match","word_match",1,5,5,3,true).activityXp)
        assertEquals(1,db.rewardDao().all().count { it.kind == "perfect" })
    }
    @Test fun verifiedMasteryNeedsThreeDistinctPositiveRetrievalDaysAndNeverPaysTwice() = runBlocking {
        val word = DatabaseSeeder.getInitialVocabulary().first().copy(id = 1)
        db.vocabularyDao().insert(word)
        rewards.review(1,"GOOD",true,true)
        assertEquals(0,db.rewardDao().all().count { it.kind == "mastery" })
        val source = "word:" + word.kasiguranin.trim().lowercase(java.util.Locale.ROOT)
        for(offset in 1..2) {
            val day = LocalDate.now().minusDays(offset.toLong()).toString()
            db.rewardDao().put(RewardReceiptEntity("retrieval:$source:$day","retrieval",source,day,0))
        }
        assertEquals(5,rewards.review(1,"GOOD",true,true).activityXp)
        assertEquals(1,db.rewardDao().all().count { it.kind == "mastery" })
        assertEquals(0,rewards.review(1,"GOOD",true,true).activityXp)
        assertEquals(1,db.rewardDao().all().count { it.kind == "category" })
    }
    @Test fun storyAndApprovalAreLifetimeRewardsWhileExtraPracticeHasNoReviewXp() = runBlocking {
        db.storyDao().insert(DatabaseSeeder.getInitialStories().first())
        assertEquals(20,rewards.story(1).activityXp)
        assertEquals(0,rewards.story(1).activityXp)
        assertTrue(db.storyDao().getStoryById(1)!!.isCompleted)
        assertEquals(10,rewards.approved(listOf("word:one")).activityXp)
        assertEquals(0,rewards.approved(listOf("word:one")).activityXp)
        db.vocabularyDao().insert(DatabaseSeeder.getInitialVocabulary().first().copy(id = 1))
        assertEquals(0,rewards.review(1,"GOOD",false,false).activityXp)
    }
    @Test fun mergingTwoDevicesKeepsOneRewardAndRestoresCompletedStories() = runBlocking {
        db.storyDao().insert(DatabaseSeeder.getInitialStories().first())
        val day = LocalDate.now().toString()
        val row = RewardReceiptEntity("story:1","story","story:1",day,20)
        rewards.mergeRewards(listOf(row,row))
        rewards.mergeRewards(listOf(row))
        assertEquals(1,db.rewardDao().all().count { it.kind == "story" })
        assertEquals(20,db.userProgressDao().getUserProgressOnce()!!.activityXp)
        assertTrue(db.storyDao().getStoryById(1)!!.isCompleted)
        assertNull(db.rewardDao().pendingCelebration().first())
    }
}
