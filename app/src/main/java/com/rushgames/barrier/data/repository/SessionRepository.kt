package com.rushgames.barrier.data.repository

import androidx.room.withTransaction
import com.rushgames.barrier.data.dao.SessionDao
import com.rushgames.barrier.data.database.BarrierDatabase
import com.rushgames.barrier.data.model.Session
import com.rushgames.barrier.data.model.SessionStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val database: BarrierDatabase
) {
    fun getAllSessions(): Flow<List<Session>> = sessionDao.getAllSessions()
    fun getActiveSession(): Flow<Session?> = sessionDao.getActiveSession()
    fun getUpcomingSessions(): Flow<List<Session>> = sessionDao.getUpcomingSessions()
    fun getSessionHistory(): Flow<List<Session>> = sessionDao.getSessionHistory()

    suspend fun createSession(session: Session): Long = sessionDao.insertSession(session)

    suspend fun createActiveSession(session: Session): Long? = database.withTransaction {
        val now = System.currentTimeMillis()
        sessionDao.completeExpiredActiveSessions(now)
        if (sessionDao.getActiveSessionOnce() != null) return@withTransaction null

        val sessionId = sessionDao.insertSession(session.copy(status = SessionStatus.IDLE))
        val activated = sessionDao.activateSessionIfAvailable(sessionId, now) > 0
        if (activated) {
            sessionId
        } else {
            sessionDao.getSessionById(sessionId)?.let { sessionDao.deleteSession(it) }
            null
        }
    }

    suspend fun startSession(sessionId: Long): Boolean = database.withTransaction {
        val now = System.currentTimeMillis()
        sessionDao.completeExpiredActiveSessions(now)
        sessionDao.activateSessionIfAvailable(sessionId, now) > 0
    }

    suspend fun completeExpiredSessions(now: Long = System.currentTimeMillis()) {
        sessionDao.completeExpiredActiveSessions(now)
    }

    suspend fun pauseSession(sessionId: Long) {
        sessionDao.updateSessionStatus(sessionId, SessionStatus.PAUSED)
    }

    suspend fun completeSession(sessionId: Long) {
        sessionDao.updateSessionStatus(sessionId, SessionStatus.COMPLETED)
    }

    suspend fun cancelSession(sessionId: Long): Boolean =
        sessionDao.cancelSessionIfAllowed(sessionId) > 0

    suspend fun failSessionStart(sessionId: Long): Boolean =
        sessionDao.cancelSessionAfterStartFailure(sessionId) > 0

    suspend fun updateSession(session: Session) = sessionDao.updateSession(session)

    suspend fun deleteSession(session: Session) = sessionDao.deleteSession(session)

    suspend fun getActiveSessionOnce(): Session? = sessionDao.getActiveSessionOnce()

    suspend fun getSessionById(id: Long): Session? = sessionDao.getSessionById(id)
}
