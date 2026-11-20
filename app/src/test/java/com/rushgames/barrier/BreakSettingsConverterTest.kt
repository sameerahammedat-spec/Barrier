package com.rushgames.barrier

import com.rushgames.barrier.data.model.BreakSettingsConverter
import com.rushgames.barrier.data.model.SessionBreakSettings
import com.rushgames.barrier.data.model.StoredDataRecoveryReporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun corruptStoredValueUsesSafeDefaultsAndReportsRecovery() {
        StoredDataRecoveryReporter.acknowledgeAfterRepair()

        assertEquals(SessionBreakSettings(), converter.fromString("{"))
        assertTrue(StoredDataRecoveryReporter.hasCorruption)
        assertTrue(StoredDataRecoveryReporter.affectedValues.value.contains("session break settings"))
    }
}
