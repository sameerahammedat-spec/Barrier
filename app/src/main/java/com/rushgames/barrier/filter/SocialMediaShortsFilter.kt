package com.rushgames.barrier.filter

import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/**
 * Feature Set B: Reels & Shorts Restrictions.
 * Detects short-form addictive video feeds (YouTube Shorts, Instagram Reels, TikTok, Facebook Reels).
 */
object SocialMediaShortsFilter {

    private val REELS_SHORTS_URL_PATTERNS = listOf(
        "youtube.com/shorts/",
        "youtu.be/shorts/",
        "instagram.com/reels/",
        "instagram.com/reel/",
        "tiktok.com/",
        "facebook.com/reel/",
        "fb.watch/"
    )

    private val REELS_SHORTS_PACKAGES = setOf(
        "com.zhiliaoapp.musically", // TikTok
        "com.ss.android.ugc.trill"  // TikTok Asia
    )

    data class ShortsFilterResult(
        val isShortsContent: Boolean,
        val platformName: String = "",
        val reason: String = ""
    )

    fun checkUrlOrPackage(url: String, packageName: String): ShortsFilterResult {
        val lowerUrl = url.lowercase(Locale.ROOT).trim()

        if (REELS_SHORTS_PACKAGES.contains(packageName)) {
            return ShortsFilterResult(true, "TikTok App", "TikTok Short-Form Video App Restricted")
        }

        for (pattern in REELS_SHORTS_URL_PATTERNS) {
            if (lowerUrl.contains(pattern)) {
                val platform = when {
                    pattern.contains("youtube") -> "YouTube Shorts"
                    pattern.contains("instagram") -> "Instagram Reels"
                    pattern.contains("facebook") || pattern.contains("fb") -> "Facebook Reels"
                    else -> "Short-Form Video Feed"
                }
                return ShortsFilterResult(true, platform, "Blocked $platform Feed")
            }
        }

        return ShortsFilterResult(false)
    }

    fun inspectAccessibilityTreeForShorts(rootNode: AccessibilityNodeInfo?): ShortsFilterResult {
        if (rootNode == null) return ShortsFilterResult(false)

        val foundText = mutableListOf<String>()
        collectNodeText(rootNode, foundText)

        for (text in foundText) {
            val lower = text.lowercase(Locale.ROOT)
            if (lower.contains("youtube shorts") || lower.contains("instagram reels") || lower.contains("remix with shorts")) {
                return ShortsFilterResult(true, "Short-Form Video UI", "Detected $text in UI")
            }
        }

        return ShortsFilterResult(false)
    }

    private fun collectNodeText(node: AccessibilityNodeInfo, list: MutableList<String>) {
        node.text?.toString()?.let { list.add(it) }
        node.contentDescription?.toString()?.let { list.add(it) }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectNodeText(it, list) }
        }
    }
}
