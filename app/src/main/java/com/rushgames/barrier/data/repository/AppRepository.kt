package com.rushgames.barrier.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.rushgames.barrier.data.model.AppInfo
import com.rushgames.barrier.data.model.KnownBrowsers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val appList = mutableListOf<AppInfo>()

        for (resolveInfo in resolveInfos) {
            val pkgName = resolveInfo.activityInfo.packageName
            // Skip Barrier itself
            if (pkgName == context.packageName) continue

            val appName = resolveInfo.loadLabel(pm).toString()
            val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isBrowser = KnownBrowsers.ALL_KNOWN_BROWSERS.contains(pkgName)

            appList.add(
                AppInfo(
                    packageName = pkgName,
                    appName = appName,
                    isSystemApp = isSystem,
                    isBrowser = isBrowser
                )
            )
        }

        appList.sortedBy { it.appName.lowercase() }
    }

    fun getAppIcon(packageName: String): Drawable? {
        return runCatching {
            context.packageManager.getApplicationIcon(packageName)
        }.getOrNull()
    }

    fun isBrowser(packageName: String): Boolean {
        return KnownBrowsers.ALL_KNOWN_BROWSERS.contains(packageName)
    }

    fun isSupportedBrowser(packageName: String): Boolean {
        return KnownBrowsers.SUPPORTED_BROWSERS.contains(packageName)
    }
}
