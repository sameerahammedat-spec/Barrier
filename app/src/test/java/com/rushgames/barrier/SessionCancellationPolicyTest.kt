package com.rushgames.barrier

import com.rushgames.barrier.data.model.Session
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCancellationPolicyTest {
    private fun session(isLocked: Boolean = false, isStrictMode: Boolean = false) = Session(
        name = "Focus",
        durationMinutes = 25,
        isLocked = isLocked,
        isStrictMode = isStrictMode
    )

    @Test
    fun ordinarySessionCanBeCancelled() {
        assertFalse(session().preventsCancellation)
    }

    @Test
    fun lockedSessionCannotBeCancelledInApp() {
        assertTrue(session(isLocked = true).preventsCancellation)
    }

    @Test
    fun strictSessionCannotBeCancelledInApp() {
        assertTrue(session(isStrictMode = true).preventsCancellation)
    }
}
