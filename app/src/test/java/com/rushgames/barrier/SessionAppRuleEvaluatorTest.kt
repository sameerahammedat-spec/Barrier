package com.rushgames.barrier

import com.rushgames.barrier.data.model.AppFilterMode
import com.rushgames.barrier.data.model.Session
import com.rushgames.barrier.data.model.SessionAppRuleEvaluator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionAppRuleEvaluatorTest {
    private fun session(
        mode: AppFilterMode,
        blocked: List<String> = emptyList(),
        allowed: List<String> = emptyList()
    ) = Session(
        name = "Test",
        durationMinutes = 30,
        appFilterMode = mode,
        blockedAppPackages = blocked,
        allowedAppPackages = allowed
    )

    @Test
    fun blockSelectedBlocksOnlySelectedPackages() {
        val rules = session(AppFilterMode.BLOCK_SELECTED, blocked = listOf("com.example.social"))
        assertTrue(SessionAppRuleEvaluator.shouldBlock("com.example.social", rules, "com.rushgames.barrier"))
        assertFalse(SessionAppRuleEvaluator.shouldBlock("com.example.mail", rules, "com.rushgames.barrier"))
    }

    @Test
    fun allowlistOnlyBlocksPackagesNotInAllowlist() {
        val rules = session(AppFilterMode.ALLOWLIST_ONLY, allowed = listOf("com.example.mail"))
        assertFalse(SessionAppRuleEvaluator.shouldBlock("com.example.mail", rules, "com.rushgames.barrier"))
        assertTrue(SessionAppRuleEvaluator.shouldBlock("com.example.social", rules, "com.rushgames.barrier"))
    }

    @Test
    fun noneModeDoesNotBlockApps() {
        assertFalse(
            SessionAppRuleEvaluator.shouldBlock(
                "com.example.social",
                session(AppFilterMode.NONE),
                "com.rushgames.barrier"
            )
        )
    }

    @Test
    fun essentialPackagesAndBarrierAreNeverBlocked() {
        val rules = session(
            AppFilterMode.BLOCK_SELECTED,
            blocked = listOf("com.rushgames.barrier", "com.android.settings", "com.android.systemui")
        )
        assertFalse(SessionAppRuleEvaluator.shouldBlock("com.rushgames.barrier", rules, "com.rushgames.barrier"))
        assertFalse(SessionAppRuleEvaluator.shouldBlock("com.android.settings", rules, "com.rushgames.barrier"))
        assertFalse(SessionAppRuleEvaluator.shouldBlock("com.android.systemui", rules, "com.rushgames.barrier"))
    }
}
