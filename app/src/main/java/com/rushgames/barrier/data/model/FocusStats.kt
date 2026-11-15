package com.rushgames.barrier.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_stats")
data class FocusStats(
    @PrimaryKey val dateKey: String, // "2024-01-15"
    val totalFocusMinutes: Int = 0,
    val sessionsCompleted: Int = 0,
    val blockedAttempts: Int = 0
)
