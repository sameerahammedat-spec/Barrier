package com.rushgames.barrier.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.rushgames.barrier.data.dao.StatsDao
import com.rushgames.barrier.data.datastore.BarrierPreferences
import com.rushgames.barrier.data.model.AttemptType
import com.rushgames.barrier.data.model.BlockedAttempt
import com.rushgames.barrier.data.model.KnownBrowsers
import com.rushgames.barrier.data.model.SessionStatus
import com.rushgames.barrier.data.model.StoredDataRecoveryReporter
import com.rushgames.barrier.data.model.WebsiteFilterMode
import com.rushgames.barrier.data.repository.SessionRepository
import com.rushgames.barrier.filter.AdultContentFilter
import com.rushgames.barrier.filter.Level1UrlFilter
import com.rushgames.barrier.filter.WebsiteDomainMatcher
import com.rushgames.barrier.ui.screens.restriction.RestrictionActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BarrierAccessibilityService : AccessibilityService() {

    companion object {
        private val mutableConnectionStatus = MutableStateFlow(false)
        val connectionStatus: StateFlow<Boolean> = mutableConnectionStatus.asStateFlow()
    }

    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var preferences: BarrierPreferences
    @Inject lateinit var statsDao: StatsDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastInspectedUrl: String? = null
    private var lastUrlInspectTime: Long = 0

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Least-privilege scope: Accessibility is used only for website checks
        // in Barrier-supported browsers. App blocking is handled by Usage Access.
        val scopedInfo = serviceInfo
        scopedInfo.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        scopedInfo.packageNames = KnownBrowsers.SUPPORTED_BROWSERS.toTypedArray()
        scopedInfo.flags = 0
        scopedInfo.notificationTimeout = 300
        setServiceInfo(scopedInfo)

        mutableConnectionStatus.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName !in KnownBrowsers.SUPPORTED_BROWSERS) return
        inspectBrowserUrl(packageName)
    }

    private fun inspectBrowserUrl(packageName: String) {
        val now = System.currentTimeMillis()

        serviceScope.launch {
            var activeSession = sessionRepository.getActiveSessionOnce()
            if (preferences.storageRecoveryNotice.first() != null || StoredDataRecoveryReporter.hasCorruption) {
                activeSession = null
            }
            if (activeSession != null && now > activeSession.endTime) {
                sessionRepository.completeSession(activeSession.id)
                activeSession = null
            }
            if (activeSession?.status != SessionStatus.ACTIVE) activeSession = null

            val globalAdultProtectionEnabled = preferences.adultProtection.first()
            if (activeSession == null && !globalAdultProtectionEnabled) return@launch

            val rootNode = rootInActiveWindow ?: return@launch
            val rawUrl = findUrlInNode(rootNode) ?: return@launch

            if (rawUrl == lastInspectedUrl && (now - lastUrlInspectTime) < 2000) {
                return@launch
            }
            lastInspectedUrl = rawUrl
            lastUrlInspectTime = now

            val cleanDomain = Level1UrlFilter.extractDomain(rawUrl)
            Log.d("BarrierAccessibility", "Browser host inspected: $cleanDomain in $packageName")

            // Level 1, 2, 3 Adult Content Filtering Check
            val sessionAdultProtectionEnabled = activeSession?.let {
                it.blockAdultContent || it.websiteFilterMode == WebsiteFilterMode.ADULT_PROTECTION
            } == true
            if (globalAdultProtectionEnabled || sessionAdultProtectionEnabled) {
                val eval = AdultContentFilter.evaluateFullWebContent(
                    urlOrDomain = rawUrl,
                    rootNode = rootNode,
                    performPreflightHttp = false
                )

                if (eval.isAdult) {
                    blockWebsite(
                        cleanDomain,
                        "Adult URL/content filter (L${eval.levelTriggered}: ${eval.reason})",
                        activeSession?.id,
                        packageName
                    )
                    return@launch
                }
            }

            // User-defined website rules apply only to the active session that owns them.
            val session = activeSession ?: return@launch
            if (session.websiteFilterMode == WebsiteFilterMode.BLOCKLIST || session.websiteFilterMode == WebsiteFilterMode.COMBINED) {
                if (WebsiteDomainMatcher.matchesAny(cleanDomain, session.blockedWebsites)) {
                    blockWebsite(cleanDomain, "Blocked Website List ('${session.name}')", session.id, packageName)
                    return@launch
                }
            }

            if (session.websiteFilterMode == WebsiteFilterMode.ALLOWLIST_ONLY || session.websiteFilterMode == WebsiteFilterMode.COMBINED) {
                if (session.allowedWebsites.isNotEmpty()) {
                    val isAllowed = WebsiteDomainMatcher.matchesAny(cleanDomain, session.allowedWebsites)
                    if (!isAllowed) {
                        blockWebsite(cleanDomain, "Website Allowlist Mode ('${session.name}')", session.id, packageName)
                        return@launch
                    }
                }
            }
        }
    }

    private fun blockWebsite(domain: String, reason: String, sessionId: Long?, browserPackage: String) {
        Log.d("BarrierAccessibility", "Blocking website: $domain in $browserPackage - $reason")

        serviceScope.launch {
            statsDao.insertBlockedAttempt(
                BlockedAttempt(
                    sessionId = sessionId,
                    type = AttemptType.WEBSITE,
                    packageNameOrDomain = domain,
                    displayName = domain
                )
            )
        }

        RestrictionActivity.start(applicationContext, "Website ($domain)", reason)
    }

    private fun findUrlInNode(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null

        val text = node.text?.toString()
        if (!text.isNullOrEmpty() && (text.contains(".") || text.startsWith("http"))) {
            if (text.contains("com") || text.contains("org") || text.contains("net") || text.contains("io") || text.contains("porn") || text.contains("xxx")) {
                return text
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val childUrl = findUrlInNode(child)
            if (childUrl != null) return childUrl
        }
        return null
    }

    override fun onInterrupt() {
        Log.d("BarrierAccessibility", "Service interrupted")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        mutableConnectionStatus.value = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        mutableConnectionStatus.value = false
        serviceScope.cancel()
        super.onDestroy()
    }
}
