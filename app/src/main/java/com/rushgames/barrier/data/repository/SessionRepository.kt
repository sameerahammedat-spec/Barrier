package com.rushgames.barrier.data.repository

import com.rushgames.barrier.data.dao.SessionDao
import com.rushgames.barrier.data.model.Session
import com.rushgames.barrier.data.model.SessionStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao
) {
    fun getAllSessions(): Flow<List<Session>> = sessionDao.getAllSessions()
    fun getActiveSession(): Flow<Session?> = sessionDao.getActiveSession()
    fun getUpcomingSessions(): Flow<List<Session>> = sessionDao.getUpcomingSessions()
    fun getSessionHistory(): Flow<List<Session>> = sessionDao.getSessionHistory()

    suspend fun createSession(session: Session): Long = sessionDao.insertSession(session)

    suspend fun startSession(sessionId: Long) {
        sessionDao.updateSessionStatus(sessionId, SessionStatus.ACTIVE)
    }

    suspend fun pauseSession(sessionId: Long) {
        sessionDao.updateSessionStatus(sessionId, SessionStatus.PAUSED)
    }

    suspend fun completeSession(sessionId: Long) {
        sessionDao.updateSessionStatus(sessionId, SessionStatus.COMPLETED)
    }

    suspend fun cancelSession(sessionId: Long) {
        sessionDao.updateSessionStatus(sessionId, SessionStatus.CANCELLED)
    }

    suspend fun updateSession(session: Session) = sessionDao.updateSession(session)

    suspend fun deleteSession(session: Session) = sessionDao.deleteSession(session)

    suspend fun getActiveSessionOnce(): Session? = sessionDao.getActiveSessionOnce()

    suspend fun getSessionById(id: Long): Session? = sessionDao.getSessionById(id)
}
