package com.rushgames.barrier.service

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.rushgames.barrier.MainActivity
import com.rushgames.barrier.data.dao.StatsDao
import com.rushgames.barrier.data.datastore.BarrierPreferences
import com.rushgames.barrier.data.model.AttemptType
import com.rushgames.barrier.data.model.AppFilterMode
import com.rushgames.barrier.data.model.SessionAppRuleEvaluator
import com.rushgames.barrier.data.model.BlockedAttempt
import com.rushgames.barrier.data.model.KnownBrowsers
import com.rushgames.barrier.data.model.SessionStatus
import com.rushgames.barrier.data.model.StoredDataRecoveryReporter
import com.rushgames.barrier.data.model.WebsiteFilterMode
import com.rushgames.barrier.data.repository.BlocklistRepository
import com.rushgames.barrier.data.repository.SessionRepository
import com.rushgames.barrier.util.ProtectedAppPackages
import com.rushgames.barrier.ui.screens.restriction.RestrictionActivity
import com.rushgames.barrier.util.PermissionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class BlockingService : Service() {

    companion object {
        const val CHANNEL_ID = "barrier_blocking"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var preferences: BarrierPreferences
    @Inject lateinit var blocklistRepository: BlocklistRepository
    @Inject lateinit var statsDao: StatsDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val protectedPackages by lazy(LazyThreadSafetyMode.NONE) {
        ProtectedAppPackages.resolve(this)
    }
    private var monitoringJob: Job? = null

    private var lastBlockedPkg: String? = null
    private var lastBlockedTimestamp: Long = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitoringLoop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                if (!PermissionManager.hasUsageStatsPermission(this)) {
                    Log.w("BlockingService", "Usage Access is unavailable; stopping app-usage monitoring")
                    stopMonitoringLoop()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf(startId)
                    return START_NOT_STICKY
                }
                startForegroundNotification()
                startMonitoringLoop()
                return START_STICKY
            }
            else -> {
                stopSelf(startId)
                return START_NOT_STICKY
            }
        }
    }

    private fun startForegroundNotification() {
        val notification = buildNotification("Focus session active — monitoring app activity")
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startMonitoringLoop() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            if (preferences.storageRecoveryNotice.first() != null || StoredDataRecoveryReporter.hasCorruption) {
                Log.e("BlockingService", "Session monitoring paused until saved session data is repaired")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }

            while (isActive) {
                if (!PermissionManager.hasUsageStatsPermission(this@BlockingService)) {
                    Log.w("BlockingService", "Usage Access was revoked; stopping app-usage monitoring")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@launch
                }

                val activeSession = sessionRepository.getActiveSessionOnce()
                if (StoredDataRecoveryReporter.hasCorruption) {
                    Log.e("BlockingService", "Session rules paused because saved rule data is corrupt")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@launch
                }
                if (activeSession == null) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@launch
                }
                if (activeSession.endTime <= System.currentTimeMillis()) {
                    sessionRepository.completeSession(activeSession.id)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@launch
                }
                checkForegroundApp()
                if (StoredDataRecoveryReporter.hasCorruption) {
                    Log.e("BlockingService", "Stopping monitoring after a corrupt session value was detected")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@launch
                }
                delay(1000L) // Poll every second while an active session is enforced
            }
        }
    }

    private suspend fun checkForegroundApp() {
        val activeSession = sessionRepository.getActiveSessionOnce() ?: return
        if (StoredDataRecoveryReporter.hasCorruption) return
        val now = System.currentTimeMillis()

        if (now > activeSession.endTime) {
            sessionRepository.completeSession(activeSession.id)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        if (activeSession.status != SessionStatus.ACTIVE) return

        // Update notification countdown
        val remainingMinutes = maxOf(0L, (activeSession.endTime - now) / 60_000L)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification("${activeSession.name} active • ${remainingMinutes}m remaining")
        )

        val foregroundPackage = getForegroundPackageName() ?: return
        if (foregroundPackage == packageName) return

        val ignored = setOf(
            "com.android.systemui",
            "com.google.android.permissioncontroller",
            "com.android.settings",
            "com.android.launcher",
            "com.android.launcher3"
        )
        if (ignored.contains(foregroundPackage)) return

        if (foregroundPackage == lastBlockedPkg && (now - lastBlockedTimestamp) < 2000) {
            return
        }

        val pm = packageManager
        val appLabel = runCatching {
            val appInfo = pm.getApplicationInfo(foregroundPackage, 0)
            pm.getApplicationLabel(appInfo).toString()
        }.getOrDefault(foregroundPackage)

        // Use the same pure rule evaluator as the Accessibility enforcement path.
        if (SessionAppRuleEvaluator.shouldBlock(foregroundPackage, activeSession, packageName, protectedPackages)) {
            val reason = when (activeSession.appFilterMode) {
                AppFilterMode.BLOCK_SELECTED -> "Selected app blocked during session '${activeSession.name}'"
                AppFilterMode.ALLOWLIST_ONLY -> "App is not in the session allowlist '${activeSession.name}'"
                AppFilterMode.NONE -> "App blocked by session rule"
            }
            triggerBlock(foregroundPackage, appLabel, reason, activeSession.id)
            return
        }

        // 2. Unapproved Browser Check
        if (KnownBrowsers.ALL_KNOWN_BROWSERS.contains(foregroundPackage)) {
            if (!KnownBrowsers.SUPPORTED_BROWSERS.contains(foregroundPackage)) {
                triggerBlock(foregroundPackage, appLabel, "Unsupported Browser strictly blocked during session", activeSession.id)
                return
            }
        }

        // 3. Custom Blocklists Check
        val blocklistIds = activeSession.blocklistIds.mapNotNull { it.toLongOrNull() }
        if (blocklistIds.isNotEmpty()) {
            val blockedPackages = blocklistRepository.getBlockedPackageNamesForBlocklists(blocklistIds)
            if (blockedPackages.contains(foregroundPackage)) {
                triggerBlock(foregroundPackage, appLabel, "Blocked by session blocklist ('${activeSession.name}')", activeSession.id)
                return
            }
        }
    }

    private suspend fun triggerBlock(pkg: String, label: String, reason: String, sessionId: Long) {
        lastBlockedPkg = pkg
        lastBlockedTimestamp = System.currentTimeMillis()

        statsDao.insertBlockedAttempt(
            BlockedAttempt(
                sessionId = sessionId,
                type = AttemptType.APP,
                packageNameOrDomain = pkg,
                displayName = label
            )
        )

        RestrictionActivity.start(applicationContext, label, reason)
    }

    private fun getForegroundPackageName(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 5000L

        return try {
            val events = usm.queryEvents(startTime, endTime) ?: return null
            val event = UsageEvents.Event()
            var lastPackage: String? = null

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastPackage = event.packageName
                }
            }
            lastPackage
        } catch (cause: SecurityException) {
            Log.w("BlockingService", "Usage Access was revoked while reading app events", cause)
            null
        }
    }

    private fun stopMonitoringLoop() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private fun buildNotification(message: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Barrier Session Monitoring")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Barrier Session",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shown during active blocking sessions"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        stopMonitoringLoop()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
