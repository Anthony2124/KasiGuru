package com.kasiguru.data.repository

import com.kasiguru.data.local.dao.ProfileDao
import com.kasiguru.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The profile roster (see [ProfileEntity] for what this does and does not cover yet). Switching
 * the active profile today only changes which name/avatar is marked active - it does not yet
 * swap [UserProgressEntity], which remains one shared row per device.
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao
) {
    fun getAllProfiles(): Flow<List<ProfileEntity>> = profileDao.getAllProfiles()

    suspend fun getActiveProfile(): ProfileEntity? = profileDao.getActiveProfile()

    suspend fun hasAnyProfile(): Boolean = profileDao.getProfileCount() > 0

    /** The first profile ever created on a device becomes active automatically. */
    suspend fun createProfile(name: String, residentName: String): Long {
        val isFirst = profileDao.getProfileCount() == 0
        val id = profileDao.insert(
            ProfileEntity(name = name, residentName = residentName, isActive = isFirst)
        )
        return id
    }

    suspend fun setActiveProfile(id: Int) {
        profileDao.clearActive()
        profileDao.setActive(id)
    }

    suspend fun deleteProfile(id: Int) = profileDao.delete(id)
}
