package com.rushgames.barrier.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage_limits")
data class AppUsageLimit(
    @PrimaryKey val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int = 0, // 0 = no limit
    val dailyLaunchLimit: Int = 0,  // 0 = no limit
    val currentUsageMinutesToday: Int = 0,
    val currentLaunchCountToday: Int = 0,
    val isEnabled: Boolean = true
)
