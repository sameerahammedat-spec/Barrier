package com.rushgames.barrier.data.dao

import androidx.room.*
import com.rushgames.barrier.data.model.Session
import com.rushgames.barrier.data.model.SessionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE status = 'ACTIVE' LIMIT 1")
    fun getActiveSession(): Flow<Session?>

    @Query("SELECT * FROM sessions WHERE status = 'ACTIVE' ORDER BY endTime ASC LIMIT 1")
    suspend fun getActiveSessionOnce(): Session?

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): Session?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session): Long

    @Update
    suspend fun updateSession(session: Session)

    @Delete
    suspend fun deleteSession(session: Session)

    @Query("UPDATE sessions SET status = :status WHERE id = :id")
    suspend fun updateSessionStatus(id: Long, status: SessionStatus)

    @Query("UPDATE sessions SET status = 'COMPLETED' WHERE status = 'ACTIVE' AND endTime <= :now")
    suspend fun completeExpiredActiveSessions(now: Long): Int

    @Query("""
        UPDATE sessions
        SET status = 'ACTIVE'
        WHERE id = :id
          AND status IN ('IDLE', 'ACTIVE')
          AND endTime > :now
          AND NOT EXISTS (
              SELECT 1 FROM sessions
              WHERE status = 'ACTIVE' AND id != :id AND endTime > :now
          )
        """)
    suspend fun activateSessionIfAvailable(id: Long, now: Long): Int

    @Query("UPDATE sessions SET status = 'CANCELLED' WHERE id = :id AND status = 'ACTIVE' AND isLocked = 0 AND isStrictMode = 0")
    suspend fun cancelSessionIfAllowed(id: Long): Int

    @Query("UPDATE sessions SET status = 'CANCELLED' WHERE id = :id AND status = 'ACTIVE'")
    suspend fun cancelSessionAfterStartFailure(id: Long): Int

    @Query("SELECT * FROM sessions WHERE status IN ('IDLE','ACTIVE') ORDER BY startTime ASC")
    fun getUpcomingSessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE status IN ('COMPLETED','CANCELLED') ORDER BY startTime DESC LIMIT 20")
    fun getSessionHistory(): Flow<List<Session>>
}
