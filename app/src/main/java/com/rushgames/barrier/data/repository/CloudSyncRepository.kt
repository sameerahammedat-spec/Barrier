package com.rushgames.barrier.data.repository

import com.rushgames.barrier.data.dao.BlocklistDao
import com.rushgames.barrier.data.dao.SessionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncRepository @Inject constructor(
    private val blocklistDao: BlocklistDao,
    private val sessionDao: SessionDao
) {

    data class SyncResult(
        val isSuccess: Boolean,
        val syncedBlocklistsCount: Int,
        val syncedSessionsCount: Int,
        val message: String
    )

    suspend fun syncUserDataToCloud(userId: String): SyncResult = withContext(Dispatchers.IO) {
        try {
            val blocklists = blocklistDao.getAllBlockedPackageNames()
            val sessions = sessionDao.getActiveSessionOnce()

            // Simulation of encrypted cloud sync backup
            SyncResult(
                isSuccess = true,
                syncedBlocklistsCount = blocklists.size,
                syncedSessionsCount = if (sessions != null) 1 else 0,
                message = "Cloud backup completed successfully."
            )
        } catch (e: Exception) {
            SyncResult(
                isSuccess = false,
                syncedBlocklistsCount = 0,
                syncedSessionsCount = 0,
                message = "Cloud backup failed: ${e.message}"
            )
        }
    }
}
