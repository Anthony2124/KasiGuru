package com.kasiguru.data.local.dao

import androidx.room.*
import com.kasiguru.data.local.entity.PublicProfileCacheEntity

@Dao
interface PublicProfileCacheDao {
    @Query("SELECT * FROM public_profile_cache WHERE uid = :uid") suspend fun get(uid: String): PublicProfileCacheEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(profile: PublicProfileCacheEntity)
    @Query("DELETE FROM public_profile_cache WHERE uid = :uid") suspend fun delete(uid: String)
}
