
package com.rushgames.barrier.data.model

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BreakSettingsConverter {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @TypeConverter
    fun fromString(value: String): SessionBreakSettings {
        return runCatching {
            json.decodeFromString<SessionBreakSettings>(value)
        }.getOrDefault(SessionBreakSettings())
    }

    @TypeConverter
    fun toString(settings: SessionBreakSettings): String {
        return json.encodeToString(settings)
    }
}
