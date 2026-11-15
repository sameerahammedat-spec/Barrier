package com.rushgames.barrier.data.repository

import com.rushgames.barrier.data.dao.BlocklistDao
import com.rushgames.barrier.data.model.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlocklistRepository @Inject constructor(
    private val blocklistDao: BlocklistDao
) {
    fun getAllBlocklists(): Flow<List<Blocklist>> = blocklistDao.getAllBlocklists()

    fun getAppsForBlocklist(blocklistId: Long): Flow<List<BlockedApp>> =
        blocklistDao.getAppsForBlocklist(blocklistId)

    fun getWebsitesForBlocklist(blocklistId: Long): Flow<List<BlockedWebsite>> =
        blocklistDao.getWebsitesForBlocklist(blocklistId)

    suspend fun createBlocklist(name: String, category: BlocklistCategory = BlocklistCategory.CUSTOM): Long =
        blocklistDao.insertBlocklist(Blocklist(name = name, category = category))

    suspend fun updateBlocklist(blocklist: Blocklist) = blocklistDao.updateBlocklist(blocklist)

    suspend fun deleteBlocklist(blocklist: Blocklist) = blocklistDao.deleteBlocklist(blocklist)

    suspend fun addAppToBlocklist(packageName: String, appName: String, blocklistId: Long) {
        blocklistDao.insertBlockedApp(BlockedApp(packageName, appName, blocklistId))
        refreshCounts(blocklistId)
    }

    suspend fun removeAppFromBlocklist(app: BlockedApp) {
        blocklistDao.deleteBlockedApp(app)
        refreshCounts(app.blocklistId)
    }

    suspend fun addWebsiteToBlocklist(domain: String, blocklistId: Long, isAllowlisted: Boolean = false) {
        blocklistDao.insertBlockedWebsite(BlockedWebsite(domain = domain, blocklistId = blocklistId, isAllowlisted = isAllowlisted))
        refreshCounts(blocklistId)
    }

    suspend fun removeWebsiteFromBlocklist(website: BlockedWebsite) {
        blocklistDao.deleteBlockedWebsite(website)
        refreshCounts(website.blocklistId)
    }

    suspend fun getBlockedPackageNamesForBlocklists(blocklistIds: List<Long>): List<String> =
        blocklistDao.getBlockedPackageNamesForBlocklists(blocklistIds)

    suspend fun getAllBlockedPackageNames(): List<String> =
        blocklistDao.getAllBlockedPackageNames()

    private suspend fun refreshCounts(blocklistId: Long) {
        val bl = blocklistDao.getBlocklistById(blocklistId) ?: return
        val appCount = blocklistDao.getAppCount(blocklistId)
        val websiteCount = blocklistDao.getWebsiteCount(blocklistId)
        blocklistDao.updateBlocklist(bl.copy(appCount = appCount, websiteCount = websiteCount))
    }

    /** Seed default blocklists on first launch */
    suspend fun seedDefaultBlocklists() {
        val id1 = blocklistDao.insertBlocklist(
            Blocklist(name = "Social Media", category = BlocklistCategory.SOCIAL_MEDIA)
        )
        blocklistDao.insertBlockedApp(BlockedApp("com.instagram.android", "Instagram", id1))
        blocklistDao.insertBlockedApp(BlockedApp("com.facebook.katana", "Facebook", id1))
        blocklistDao.insertBlockedApp(BlockedApp("com.twitter.android", "X (Twitter)", id1))
        blocklistDao.insertBlockedApp(BlockedApp("com.snapchat.android", "Snapchat", id1))
        blocklistDao.insertBlockedApp(BlockedApp("com.zhiliaoapp.musically", "TikTok", id1))

        val id2 = blocklistDao.insertBlocklist(
            Blocklist(name = "Entertainment", category = BlocklistCategory.ENTERTAINMENT)
        )
        blocklistDao.insertBlockedApp(BlockedApp("com.netflix.mediaclient", "Netflix", id2))
        blocklistDao.insertBlockedApp(BlockedApp("com.google.android.youtube", "YouTube", id2))
        blocklistDao.insertBlockedApp(BlockedApp("com.amazon.avod.thirdpartyclient", "Prime Video", id2))

        val id3 = blocklistDao.insertBlocklist(
            Blocklist(name = "Gaming", category = BlocklistCategory.GAMING)
        )

        // Update counts
        for (id in listOf(id1, id2, id3)) refreshCounts(id)
    }
}
