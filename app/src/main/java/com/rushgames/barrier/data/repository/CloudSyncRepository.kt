package com.rushgames.barrier.data.repository

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reports cloud sync as unavailable until a real authenticated cloud backend is configured.
 */
@Singleton
class CloudSyncRepository @Inject constructor() {

    data class SyncResult(
        val isSuccess: Boolean,
        val syncedBlocklistsCount: Int,
        val syncedSessionsCount: Int,
        val message: String
    )

    @Suppress("UNUSED_PARAMETER")
    suspend fun syncUserDataToCloud(userId: String): SyncResult = SyncResult(
        isSuccess = false,
        syncedBlocklistsCount = 0,
        syncedSessionsCount = 0,
        message = "Cloud sync is unavailable: no cloud service is configured."
    )
}
