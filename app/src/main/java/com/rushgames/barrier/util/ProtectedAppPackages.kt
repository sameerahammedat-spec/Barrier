package com.rushgames.barrier.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telecom.TelecomManager

/**
 * Resolves device-specific packages that must never be included in a focus-session block rule.
 * In particular, the current HOME app and default dialer vary by device manufacturer.
 */
object ProtectedAppPackages {
    private val essentialPackages = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.android.phone",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.contacts",
        "com.google.android.contacts",
        "com.android.emergency",
        "com.android.mms",
        "com.google.android.apps.messaging"
    )

    fun resolve(context: Context): Set<String> {
        val packages = essentialPackages.toMutableSet()
        packages += context.packageName

        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        runCatching {
            context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
        }.getOrNull()?.let(packages::add)

        runCatching {
            (context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)
                ?.defaultDialerPackage
        }.getOrNull()?.let(packages::add)

        return packages
    }
}
