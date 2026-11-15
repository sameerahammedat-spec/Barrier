package com.rushgames.barrier.filter

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Robust 3-Level Adult Content Filter Engine for Barrier:
 *
 * Level 1: URL & Domain matching (Keywords, TLDs, search engine query inspection, decodes obfuscation)
 * Level 2: Search & Access Adult Content Database (O(1) hash / Trie adult host database & local Room DB)
 * Level 3: Deep Web Content Extractor & Analyzer (Extracts headers, body text, tables, images, video streams & scores adult density before opening/rendering)
 */
object AdultContentFilter {

    private const val TAG = "AdultContentFilter"

    val SAFESEARCH_GOOGLE_IP = "216.239.38.120"
    val RESTRICTED_YOUTUBE_IP = "216.239.38.119"

    private val level2DatabaseFilter = Level2DatabaseFilter()

    data class ContentFilterEvaluation(
        val isAdult: Boolean,
        val levelTriggered: Int, // 1, 2, or 3
        val reason: String,
        val score: Int = 0
    )

    /**
     * Fast Synchronous Evaluation (Level 1 & Level 2)
     * Used by VPN Service for instant sub-millisecond DNS query filtering.
     */
    fun evaluateUrlOrDomainFast(urlOrDomain: String, customDatabaseDomains: Set<String> = emptySet()): ContentFilterEvaluation {
        // Level 1: URL & Domain Matching
        val l1Result = Level1UrlFilter.checkUrlOrDomain(urlOrDomain)
        if (l1Result.isBlocked) {
            Log.d(TAG, "Level 1 URL Filter blocked: $urlOrDomain -> ${l1Result.reason}")
            return ContentFilterEvaluation(
                isAdult = true,
                levelTriggered = 1,
                reason = l1Result.reason,
                score = 100
            )
        }

        // Level 2: Search & Access Adult Content Database
        val domain = Level1UrlFilter.extractDomain(urlOrDomain)
        val l2Result = level2DatabaseFilter.isDomainInDatabase(domain, customDatabaseDomains)
        if (l2Result.isBlocked) {
            Log.d(TAG, "Level 2 Database Filter blocked: $urlOrDomain -> ${l2Result.reason}")
            return ContentFilterEvaluation(
                isAdult = true,
                levelTriggered = 2,
                reason = l2Result.reason,
                score = 100
            )
        }

        return ContentFilterEvaluation(
            isAdult = false,
            levelTriggered = 0,
            reason = "Clean URL & Database Search"
        )
    }

    /**
     * Comprehensive 3-Level Evaluation (Level 1, Level 2, and Level 3 Deep Content Extraction)
     * Used by Accessibility Service & Web inspection before opening or displaying web pages.
     */
    suspend fun evaluateFullWebContent(
        urlOrDomain: String,
        rootNode: AccessibilityNodeInfo? = null,
        performPreflightHttp: Boolean = true,
        customDatabaseDomains: Set<String> = emptySet()
    ): ContentFilterEvaluation = withContext(Dispatchers.IO) {
        // 1. Level 1 Fast Check
        val fastCheck = evaluateUrlOrDomainFast(urlOrDomain, customDatabaseDomains)
        if (fastCheck.isAdult) return@withContext fastCheck

        // 2. Level 3: Extract Content from Accessibility Service Node Tree (Headers, Body, Images, Tables)
        if (rootNode != null) {
            val nodeContent = Level3ContentAnalyzer.extractFromAccessibilityNode(rootNode, urlOrDomain)
            val nodeAnalysis = Level3ContentAnalyzer.analyzeContent(nodeContent)
            if (nodeAnalysis.isAdultContent) {
                Log.d(TAG, "Level 3 Accessibility Node Analyzer blocked: $urlOrDomain -> ${nodeAnalysis.reason}")
                return@withContext ContentFilterEvaluation(
                    isAdult = true,
                    levelTriggered = 3,
                    reason = "Level 3 Node Analysis: ${nodeAnalysis.reason}",
                    score = nodeAnalysis.score
                )
            }
        }

        // 3. Level 3: HTTP Pre-flight Content Extraction & Analysis (Headers, Meta tags, HTML Body, Video streams)
        if (performPreflightHttp && (urlOrDomain.startsWith("http://") || urlOrDomain.startsWith("https://"))) {
            val httpContent = Level3ContentAnalyzer.extractFromHttpPreflight(urlOrDomain)
            val httpAnalysis = Level3ContentAnalyzer.analyzeContent(httpContent)
            if (httpAnalysis.isAdultContent) {
                Log.d(TAG, "Level 3 HTTP Preflight Analyzer blocked: $urlOrDomain -> ${httpAnalysis.reason}")
                return@withContext ContentFilterEvaluation(
                    isAdult = true,
                    levelTriggered = 3,
                    reason = "Level 3 Deep Extractor: ${httpAnalysis.reason}",
                    score = httpAnalysis.score
                )
            }
        }

        return@withContext ContentFilterEvaluation(
            isAdult = false,
            levelTriggered = 0,
            reason = "Passed all 3 adult content filtering levels"
        )
    }

    // Helper functions
    fun isAdultDomain(domain: String): Boolean = evaluateUrlOrDomainFast(domain).isAdult

    fun isSearchEngineDomain(domain: String): Boolean {
        val lower = domain.lowercase()
        return lower.contains("google.com") || lower.contains("bing.com") || lower.contains("duckduckgo.com")
    }

    fun isYouTubeDomain(domain: String): Boolean {
        val lower = domain.lowercase()
        return lower.contains("youtube.com") || lower.contains("youtu.be")
    }
}
