package com.rushgames.barrier.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


enum class SessionStatus { IDLE, ACTIVE, PAUSED, COMPLETED, CANCELLED }
enum class SessionScheduleType { MANUAL, SCHEDULED, RECURRING }
enum class WebsiteFilterMode { BLOCKLIST, ALLOWLIST_ONLY, ADULT_PROTECTION, COMBINED }

@Serializable
data class SessionBreakSettings(
    val allowBreaks: Boolean = false,
    val breakDurationMinutes: Int = 5,
    val breaksPerSession: Int = 1
)

class StringListConverter {
    private val json = Json

    @TypeConverter
    fun fromString(value: String): List<String> {
        if (value.isEmpty()) return emptyList()
        if (!value.startsWith(JSON_PREFIX)) return value.split('|')

        return runCatching {
            json.decodeFromString<List<String>>(value.removePrefix(JSON_PREFIX))
        }.getOrElse { cause ->
            throw IllegalArgumentException("Stored session string list is invalid", cause)
        }
    }

    @TypeConverter
    fun toString(list: List<String>): String = JSON_PREFIX + json.encodeToString(list)

    private companion object {
        const val JSON_PREFIX = "json:"
    }
}


@Entity(tableName = "sessions")
@TypeConverters(StringListConverter::class, BreakSettingsConverter::class)
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val durationMinutes: Int,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = startTime + durationMinutes * 60_000L,
    val status: SessionStatus = SessionStatus.IDLE,
    val scheduleType: SessionScheduleType = SessionScheduleType.MANUAL,
    val isLocked: Boolean = false,
    val isStrictMode: Boolean = false,
    val websiteFilterMode: WebsiteFilterMode = WebsiteFilterMode.BLOCKLIST,
    // Comma-separated blocklist IDs
    val blocklistIds: List<String> = emptyList(),
    // Apps allowed during this session (package names)
    val allowedAppPackages: List<String> = emptyList(),
    // Websites allowed during this session
    val allowedWebsites: List<String> = emptyList(),
    // Blocked websites for this session
    val blockedWebsites: List<String> = emptyList(),
    val breakSettings: SessionBreakSettings = SessionBreakSettings(),
    val blockAdultContent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val preventsCancellation: Boolean
        get() = isLocked || isStrictMode
}
