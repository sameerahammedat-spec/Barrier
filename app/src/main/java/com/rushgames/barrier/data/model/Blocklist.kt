package com.rushgames.barrier.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BlocklistCategory {
    CUSTOM, SOCIAL_MEDIA, NEWS, ENTERTAINMENT, GAMING, ADULT, SHOPPING, PRODUCTIVITY, OTHER
}

@Entity(tableName = "blocklists")
data class Blocklist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: BlocklistCategory = BlocklistCategory.CUSTOM,
    val isEnabled: Boolean = true,
    val appCount: Int = 0,
    val websiteCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
