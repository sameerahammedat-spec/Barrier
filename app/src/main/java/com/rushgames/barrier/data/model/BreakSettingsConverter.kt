
package com.rushgames.barrier.data.model

import androidx.room.TypeConverter
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BreakSettingsConverter {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @TypeConverter
    fun fromString(value: String): SessionBreakSettings = try {
        json.decodeFromString<SessionBreakSettings>(value)
    } catch (cause: SerializationException) {
        throw IllegalArgumentException("Stored session break settings are invalid", cause)
    }

    @TypeConverter
    fun toString(settings: SessionBreakSettings): String {
        return json.encodeToString(settings)
    }
}
