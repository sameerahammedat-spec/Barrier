package com.rushgames.barrier

import com.rushgames.barrier.data.model.SessionDurationPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionDurationPolicyTest {
    @Test
    fun convertsHoursAndMinutesToTotalMinutes() {
        assertEquals(25, SessionDurationPolicy.toMinutes(0, 25))
        assertEquals(90, SessionDurationPolicy.toMinutes(1, 30))
        assertEquals(10_080, SessionDurationPolicy.toMinutes(168, 0))
    }

    @Test
    fun acceptsMinimumAndMaximumDurations() {
        assertTrue(SessionDurationPolicy.isValid(5))
        assertTrue(SessionDurationPolicy.isValid(10_080))
    }

    @Test
    fun rejectsDurationsOutsideSupportedRange() {
        assertFalse(SessionDurationPolicy.isValid(0))
        assertFalse(SessionDurationPolicy.isValid(4))
        assertFalse(SessionDurationPolicy.isValid(10_081))
    }
}
