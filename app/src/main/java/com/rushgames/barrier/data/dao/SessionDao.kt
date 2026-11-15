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

    @Query("SELECT * FROM sessions WHERE status = 'ACTIVE' LIMIT 1")
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

    @Query("SELECT * FROM sessions WHERE status IN ('IDLE','ACTIVE') ORDER BY startTime ASC")
    fun getUpcomingSessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE status IN ('COMPLETED','CANCELLED') ORDER BY startTime DESC LIMIT 20")
    fun getSessionHistory(): Flow<List<Session>>
}
