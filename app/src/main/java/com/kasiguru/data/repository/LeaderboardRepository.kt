package com.kasiguru.data.repository

import com.kasiguru.data.local.DatabaseSeeder
import com.kasiguru.data.local.dao.LeaderboardDao
import com.kasiguru.data.local.entity.LeaderboardEntity
import com.kasiguru.util.gamification.GamificationEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaderboardRepository @Inject constructor(
    private val leaderboardDao: LeaderboardDao,
    private val userProgressRepository: UserProgressRepository
) {
    /**
     * Get Leaderboard ranked by XP, dynamically incorporating active user progress.
     */
    fun getLeaderboardByXp(): Flow<List<LeaderboardEntity>> {
        return combine(
            leaderboardDao.getLeaderboardByXp(),
            userProgressRepository.getUserProgress()
        ) { rawList, userProgress ->
            val list = if (rawList.isEmpty()) DatabaseSeeder.getInitialLeaderboard().toMutableList() else rawList.toMutableList()

            if (userProgress != null) {
                val levelInfo = GamificationEngine.getLevelInfo(userProgress.totalXp)
                val userEntry = LeaderboardEntity(
                    id = 1,
                    name = if (userProgress.fullName.isNotEmpty()) userProgress.fullName else userProgress.userName,
                    totalXp = userProgress.totalXp,
                    currentStreak = userProgress.currentStreak,
                    avatarIconId = userProgress.profileIconId,
                    levelTitle = levelInfo.title,
                    isCurrentUser = true
                )

                // Replace or insert current user
                list.removeAll { it.isCurrentUser || it.id == 1 }
                list.add(userEntry)
            }

            list.sortedByDescending { it.totalXp }
        }
    }

    /**
     * Get Leaderboard ranked by Daily Streak.
     */
    fun getLeaderboardByStreak(): Flow<List<LeaderboardEntity>> {
        return combine(
            leaderboardDao.getLeaderboardByStreak(),
            userProgressRepository.getUserProgress()
        ) { rawList, userProgress ->
            val list = if (rawList.isEmpty()) DatabaseSeeder.getInitialLeaderboard().toMutableList() else rawList.toMutableList()

            if (userProgress != null) {
                val levelInfo = GamificationEngine.getLevelInfo(userProgress.totalXp)
                val userEntry = LeaderboardEntity(
                    id = 1,
                    name = if (userProgress.fullName.isNotEmpty()) userProgress.fullName else userProgress.userName,
                    totalXp = userProgress.totalXp,
                    currentStreak = userProgress.currentStreak,
                    avatarIconId = userProgress.profileIconId,
                    levelTitle = levelInfo.title,
                    isCurrentUser = true
                )

                list.removeAll { it.isCurrentUser || it.id == 1 }
                list.add(userEntry)
            }

            list.sortedByDescending { it.currentStreak }
        }
    }
}
