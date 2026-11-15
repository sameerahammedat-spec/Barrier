package com.rushgames.barrier.data.dao

import androidx.room.*
import com.rushgames.barrier.data.model.BlockedAttempt
import com.rushgames.barrier.data.model.FocusStats
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStats(stats: FocusStats)

    @Query("SELECT * FROM focus_stats WHERE dateKey = :dateKey")
    suspend fun getStatsByDate(dateKey: String): FocusStats?

    @Query("SELECT * FROM focus_stats ORDER BY dateKey DESC LIMIT 7")
    fun getRecentStats(): Flow<List<FocusStats>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedAttempt(attempt: BlockedAttempt)

    @Query("SELECT * FROM blocked_attempts ORDER BY timestamp DESC LIMIT 100")
    fun getBlockedAttempts(): Flow<List<BlockedAttempt>>

    @Query("SELECT COUNT(*) FROM blocked_attempts WHERE timestamp >= :sinceMs")
    suspend fun getBlockedAttemptCount(sinceMs: Long): Int

    @Query("DELETE FROM blocked_attempts WHERE timestamp < :beforeMs")
    suspend fun pruneOldAttempts(beforeMs: Long)
}
