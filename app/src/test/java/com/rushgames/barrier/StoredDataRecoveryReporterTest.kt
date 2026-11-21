package com.rushgames.barrier

import com.rushgames.barrier.data.model.StoredDataRecoveryReporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StoredDataRecoveryReporterTest {

    @Before
    fun resetReporter() {
        StoredDataRecoveryReporter.acknowledgeAfterRepair()
    }

    @Test
    fun recordsDistinctAffectedValueCategories() {
        StoredDataRecoveryReporter.report("session rule lists")
        StoredDataRecoveryReporter.report("session break settings")
        StoredDataRecoveryReporter.report("session rule lists")

        assertTrue(StoredDataRecoveryReporter.hasCorruption)
        assertEquals(
            setOf("session rule lists", "session break settings"),
            StoredDataRecoveryReporter.affectedValues.value
        )
    }

    @Test
    fun acknowledgementClearsCurrentRecoverySignal() {
        StoredDataRecoveryReporter.report("session rule lists")
        StoredDataRecoveryReporter.acknowledgeAfterRepair()

        assertFalse(StoredDataRecoveryReporter.hasCorruption)
        assertEquals(emptySet<String>(), StoredDataRecoveryReporter.affectedValues.value)
    }
}
