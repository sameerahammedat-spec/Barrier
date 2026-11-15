package com.rushgames.barrier.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AttemptType { APP, WEBSITE, BROWSER }

@Entity(tableName = "blocked_attempts")
data class BlockedAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long?,
    val type: AttemptType,
    val packageNameOrDomain: String,
    val displayName: String,
    val timestamp: Long = System.currentTimeMillis()
)
