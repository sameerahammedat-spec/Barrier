package com.rushgames.barrier

import com.rushgames.barrier.data.model.Session
import com.rushgames.barrier.ui.screens.protection.ProtectionCenterUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionCenterUiStateTest {

    @Test
    fun sessionRulesRequireAnActiveSessionAndAccessibilityPermission() {
        val activeSession = Session(
            name = "Focus",
            durationMinutes = 30,
            endTime = System.currentTimeMillis() + 60_000,
            blockedWebsites = listOf("example.com")
        )

        assertTrue(ProtectionCenterUiState(activeSession, accessibilityEnabled = true, accessibilityServiceConnected = true).sessionRulesEnforced)
        assertFalse(ProtectionCenterUiState(activeSession, accessibilityEnabled = false, accessibilityServiceConnected = true).sessionRulesEnforced)
        assertFalse(ProtectionCenterUiState(activeSession = null, accessibilityEnabled = true, accessibilityServiceConnected = true).sessionRulesEnforced)
    }

    @Test
    fun enabledAccessibilityPermissionWithoutConnectedServiceIsNotReportedAsEnforced() {
        val activeSession = Session(
            name = "Focus",
            durationMinutes = 30,
            endTime = System.currentTimeMillis() + 60_000,
            blockedWebsites = listOf("example.com")
        )
        assertFalse(
            ProtectionCenterUiState(
                activeSession = activeSession,
                accessibilityEnabled = true,
                accessibilityServiceConnected = false
            ).sessionRulesEnforced
        )
        assertFalse(
            ProtectionCenterUiState(
                adultProtectionEnabled = true,
                accessibilityEnabled = true,
                accessibilityServiceConnected = false
            ).globalAdultFilteringEnforced
        )
    }

    @Test
    fun sessionWithoutSelectedRulesIsNotReportedAsEnforced() {
        val session = Session(name = "Empty", durationMinutes = 30, endTime = System.currentTimeMillis() + 60_000)

        assertFalse(ProtectionCenterUiState(session, accessibilityEnabled = true, accessibilityServiceConnected = true).sessionRulesEnforced)
    }

    @Test
    fun globalAdultFilteringIsReportedActiveOnlyWhenEnabledAndAccessibilityIsGranted() {
        assertTrue(ProtectionCenterUiState(adultProtectionEnabled = true, accessibilityEnabled = true, accessibilityServiceConnected = true).globalAdultFilteringEnforced)
        assertFalse(ProtectionCenterUiState(adultProtectionEnabled = true, accessibilityEnabled = false, accessibilityServiceConnected = true).globalAdultFilteringEnforced)
        assertFalse(ProtectionCenterUiState(adultProtectionEnabled = false, accessibilityEnabled = true, accessibilityServiceConnected = true).globalAdultFilteringEnforced)
    }

    @Test
    fun expiredSessionIsNotReportedAsEnforced() {
        val expiredSession = Session(name = "Finished", durationMinutes = 30, endTime = System.currentTimeMillis() - 1)

        assertFalse(ProtectionCenterUiState(expiredSession, accessibilityEnabled = true, accessibilityServiceConnected = true).sessionRulesEnforced)
    }
}
