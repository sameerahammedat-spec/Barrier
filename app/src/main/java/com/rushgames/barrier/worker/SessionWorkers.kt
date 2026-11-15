package com.rushgames.barrier.worker

import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.*
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
        val sessionId = inputData.getLong("sessionId", -1L)
        if (sessionId == -1L) return Result.failure()

        val session = sessionRepository.getSessionById(sessionId) ?: return Result.failure()
        if (session.status == SessionStatus.ACTIVE) {
            sessionRepository.completeSession(sessionId)

            // Stop BlockingService
            val intent = Intent(context, BlockingService::class.java).apply {
                action = BlockingService.ACTION_STOP
            }
            context.startService(intent)
        }
        return Result.success()
    }

    companion object {
        fun scheduleExpiration(context: Context, sessionId: Long, delayMs: Long) {
            val data = workDataOf("sessionId" to sessionId)
            val request = OneTimeWorkRequestBuilder<SessionExpirationWorker>()
                .setInputData(data)
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
        val sessionId = inputData.getLong("sessionId", -1L)
        if (sessionId == -1L) return Result.failure()

        val session = sessionRepository.getSessionById(sessionId) ?: return Result.failure()
        sessionRepository.startSession(sessionId)

        // Start BlockingService
        val intent = Intent(context, BlockingService::class.java).apply {
            action = BlockingService.ACTION_START
        }
        context.startForegroundService(intent)

        // Schedule expiration worker
        val remainingDelay = session.endTime - System.currentTimeMillis()
        SessionExpirationWorker.scheduleExpiration(context, sessionId, remainingDelay)

        return Result.success()
    }

    companion object {
        fun scheduleStart(context: Context, sessionId: Long, delayMs: Long) {
            val data = workDataOf("sessionId" to sessionId)
            val request = OneTimeWorkRequestBuilder<SessionStartWorker>()
                .setInputData(data)
                .setInitialDelay(maxOf(0L, delayMs), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork("session_start_$sessionId", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
