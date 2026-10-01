package com.kasiguru.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "public_profile_cache")
data class PublicProfileCacheEntity(@PrimaryKey val uid: String, val payload: String)
