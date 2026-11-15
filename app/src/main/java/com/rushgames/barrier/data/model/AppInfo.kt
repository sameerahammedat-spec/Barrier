package com.rushgames.barrier.data.model

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false,
    val isBrowser: Boolean = false,
    val isBlocked: Boolean = false,
    val isAllowedInSession: Boolean = false
)

/** Known browser package names for detection */
object KnownBrowsers {
    val SUPPORTED_BROWSERS = setOf(
        "com.android.chrome",
        "com.microsoft.emmx", // Edge
        "com.google.android.googlequicksearchbox" // Google Search/app
    )

    val ALL_KNOWN_BROWSERS = setOf(
        "com.android.chrome",
        "com.microsoft.emmx",
        "com.google.android.googlequicksearchbox",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.brave.browser",
        "com.UCMobile.intl",
        "com.uc.browser.en",
        "com.kiwibrowser.browser",
        "com.sec.android.app.sbrowser", // Samsung Internet
        "com.vivaldi.browser",
        "com.duckduckgo.mobile.android",
        "com.mi.globalbrowser",
        "mark.via.gp",
        "com.puffin.browser"
    )
}
