package com.rushgames.barrier.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_websites")
data class BlockedWebsite(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val blocklistId: Long,
    val isAllowlisted: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
