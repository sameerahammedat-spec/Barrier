package com.rushgames.barrier.data.model

/**
 * Pure app-rule evaluation shared by the Accessibility and UsageStats enforcement paths.
 * Essential Android packages and Barrier itself are never blocked by session app rules.
 */
object SessionAppRuleEvaluator {
    private val protectedSystemPackages = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher"
    )

    fun shouldBlock(packageName: String, session: Session, barrierPackageName: String): Boolean {
        if (packageName == barrierPackageName || packageName in protectedSystemPackages) return false

        return when (session.appFilterMode) {
            AppFilterMode.NONE -> false
            AppFilterMode.BLOCK_SELECTED -> packageName in session.blockedAppPackages
            AppFilterMode.ALLOWLIST_ONLY -> packageName !in session.allowedAppPackages
        }
    }
}
