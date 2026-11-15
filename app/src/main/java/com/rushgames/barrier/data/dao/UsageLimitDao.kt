package com.rushgames.barrier.data.dao

import androidx.room.*
import com.rushgames.barrier.data.model.AppUsageLimit
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageLimitDao {
    @Query("SELECT * FROM app_usage_limits")
    fun getAllLimits(): Flow<List<AppUsageLimit>>

    @Query("SELECT * FROM app_usage_limits WHERE packageName = :packageName")
    suspend fun getLimitForPackage(packageName: String): AppUsageLimit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLimit(limit: AppUsageLimit)

    @Delete
    suspend fun deleteLimit(limit: AppUsageLimit)
}
