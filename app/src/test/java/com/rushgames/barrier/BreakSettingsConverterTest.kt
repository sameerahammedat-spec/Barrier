package com.rushgames.barrier

import com.rushgames.barrier.data.model.BreakSettingsConverter
import com.rushgames.barrier.data.model.SessionBreakSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class BreakSettingsConverterTest {
    private val converter = BreakSettingsConverter()

    @Test
    fun roundTripsBreakSettings() {
        val settings = SessionBreakSettings(
            allowBreaks = true,
            breakDurationMinutes = 7,
            breaksPerSession = 3
        )

        assertEquals(settings, converter.fromString(converter.toString(settings)))
    }

    @Test
    fun toleratesUnknownFieldsForForwardCompatibility() {
        assertEquals(
            SessionBreakSettings(allowBreaks = true, breakDurationMinutes = 5, breaksPerSession = 1),
            converter.fromString("""{"allowBreaks":true,"futureSetting":9}""")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun corruptStoredValueIsNotSilentlyReplacedWithDefaults() {
        converter.fromString("{")
    }
}
