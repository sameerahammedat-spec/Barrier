package com.rushgames.barrier.data.dao

import androidx.room.*
import com.rushgames.barrier.data.model.Blocklist
import com.rushgames.barrier.data.model.BlockedApp
import com.rushgames.barrier.data.model.BlockedWebsite
import kotlinx.coroutines.flow.Flow

@Dao
interface BlocklistDao {
    @Query("SELECT * FROM blocklists ORDER BY createdAt DESC")
    fun getAllBlocklists(): Flow<List<Blocklist>>

    @Query("SELECT * FROM blocklists WHERE id = :id")
    suspend fun getBlocklistById(id: Long): Blocklist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocklist(blocklist: Blocklist): Long

    @Update
    suspend fun updateBlocklist(blocklist: Blocklist)

    @Delete
    suspend fun deleteBlocklist(blocklist: Blocklist)

    @Query("SELECT * FROM blocked_apps WHERE blocklistId = :blocklistId")
    fun getAppsForBlocklist(blocklistId: Long): Flow<List<BlockedApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedApp(app: BlockedApp)

    @Delete
    suspend fun deleteBlockedApp(app: BlockedApp)

    @Query("SELECT * FROM blocked_websites WHERE blocklistId = :blocklistId")
    fun getWebsitesForBlocklist(blocklistId: Long): Flow<List<BlockedWebsite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedWebsite(website: BlockedWebsite)

    @Delete
    suspend fun deleteBlockedWebsite(website: BlockedWebsite)

    @Query("SELECT COUNT(*) FROM blocked_apps WHERE blocklistId = :blocklistId")
    suspend fun getAppCount(blocklistId: Long): Int

    @Query("SELECT COUNT(*) FROM blocked_websites WHERE blocklistId = :blocklistId")
    suspend fun getWebsiteCount(blocklistId: Long): Int

    @Query("SELECT packageName FROM blocked_apps WHERE blocklistId IN (:blocklistIds)")
    suspend fun getBlockedPackageNamesForBlocklists(blocklistIds: List<Long>): List<String>

    @Query("SELECT packageName FROM blocked_apps")
    suspend fun getAllBlockedPackageNames(): List<String>
}
