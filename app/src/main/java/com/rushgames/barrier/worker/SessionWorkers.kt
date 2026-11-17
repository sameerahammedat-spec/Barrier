package com.rushgames.barrier.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.rushgames.barrier.data.model.SessionStatus
import com.rushgames.barrier.data.repository.SessionRepository
import com.rushgames.barrier.service.BlockingService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SessionExpirationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val sessionRepository: SessionRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val sessionId = inputData.getLong(KEY_SESSION_ID, INVALID_SESSION_ID)
        if (sessionId == INVALID_SESSION_ID) return Result.failure()

        val session = sessionRepository.getSessionById(sessionId) ?: return Result.success()
        if (session.status != SessionStatus.ACTIVE) return Result.success()

        val now = System.currentTimeMillis()
        if (session.endTime > now) {
            // Wall clock can move backwards after the delay was scheduled.
            return Result.retry()
        }

        sessionRepository.completeSession(sessionId)
        if (sessionRepository.getActiveSessionOnce() == null) {
            context.stopService(Intent(context, BlockingService::class.java))
        }
        return Result.success()
    }

    companion object {
        private const val KEY_SESSION_ID = "sessionId"
        private const val INVALID_SESSION_ID = -1L

        fun scheduleExpiration(context: Context, sessionId: Long, delayMs: Long) {
            val request = OneTimeWorkRequestBuilder<SessionExpirationWorker>()
                .setInputData(workDataOf(KEY_SESSION_ID to sessionId))
                .setInitialDelay(maxOf(0L, delayMs), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork("session_expire_$sessionId", ExistingWorkPolicy.REPLACE, request)
        }
    }
}

@HiltWorker
class SessionStartWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val sessionRepository: SessionRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val sessionId = inputData.getLong(KEY_SESSION_ID, INVALID_SESSION_ID)
        if (sessionId == INVALID_SESSION_ID) return Result.failure()

        val session = sessionRepository.getSessionById(sessionId) ?: return Result.success()
        if (session.status !in setOf(SessionStatus.IDLE, SessionStatus.ACTIVE)) return Result.success()

        val now = System.currentTimeMillis()
        if (session.endTime <= now) {
            sessionRepository.completeSession(sessionId)
            return Result.success()
        }

        if (session.status == SessionStatus.IDLE && session.startTime > now) {
            deferStart(context, sessionId, session.startTime - now)
            return Result.success()
        }

        val activeSession = sessionRepository.getActiveSessionOnce()
        if (activeSession != null &&
            activeSession.id != sessionId &&
            activeSession.endTime > now
        ) {
            deferStart(context, sessionId, activeSession.endTime - now)
            return Result.success()
        }

        if (!sessionRepository.startSession(sessionId)) {
            val conflictingSession = sessionRepository.getActiveSessionOnce()
            if (conflictingSession != null &&
                conflictingSession.id != sessionId &&
                conflictingSession.endTime > now
            ) {
                deferStart(context, sessionId, conflictingSession.endTime - now)
                return Result.success()
            }
            return Result.retry()
        }

        return try {
            scheduleExpiration(context, session)
            val serviceIntent = Intent(context, BlockingService::class.java).apply {
                action = BlockingService.ACTION_START
            }
            context.startForegroundService(serviceIntent)
            Result.success()
        } catch (cause: Exception) {
            if (cause is kotlinx.coroutines.CancellationException) throw cause
            Log.e(TAG, "Could not start scheduled focus session", cause)
            sessionRepository.failSessionStart(sessionId)
            if (sessionRepository.getActiveSessionOnce() == null) {
                context.stopService(Intent(context, BlockingService::class.java))
            }
            Result.failure()
        }
    }

    private fun scheduleExpiration(context: Context, session: com.rushgames.barrier.data.model.Session) {
        SessionExpirationWorker.scheduleExpiration(
            context,
            session.id,
            session.endTime - System.currentTimeMillis()
        )
    }

    private fun deferStart(context: Context, sessionId: Long, delayMs: Long) {
        val request = OneTimeWorkRequestBuilder<SessionStartWorker>()
            .setInputData(workDataOf(KEY_SESSION_ID to sessionId))
            .setInitialDelay(maxOf(0L, delayMs), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "session_start_$sessionId",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
    }

    companion object {
        private const val TAG = "SessionStartWorker"
        private const val KEY_SESSION_ID = "sessionId"
        private const val INVALID_SESSION_ID = -1L

        fun scheduleStart(context: Context, sessionId: Long, delayMs: Long) {
            val request = OneTimeWorkRequestBuilder<SessionStartWorker>()
                .setInputData(workDataOf(KEY_SESSION_ID to sessionId))
                .setInitialDelay(maxOf(0L, delayMs), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork("session_start_$sessionId", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
