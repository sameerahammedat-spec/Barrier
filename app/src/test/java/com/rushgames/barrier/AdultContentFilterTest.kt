package com.rushgames.barrier

import android.graphics.Bitmap
import com.rushgames.barrier.filter.AdultContentFilter
import com.rushgames.barrier.filter.Level1UrlFilter
import com.rushgames.barrier.filter.Level2DatabaseFilter
import com.rushgames.barrier.filter.Level3ContentAnalyzer
import com.rushgames.barrier.filter.OnDeviceImageClassifier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdultContentFilterTest {

    @Test
    fun testLevel1UrlMatching_ExplicitDomain_Blocked() {
        val res = Level1UrlFilter.checkUrlOrDomain("https://www.pornhub.com/video")
        assertTrue(res.isBlocked)
        assertTrue(res.reason.contains("keyword"))
    }

    @Test
    fun testLevel1UrlMatching_AdultTld_Blocked() {
        val res = Level1UrlFilter.checkUrlOrDomain("http://example.xxx")
        assertTrue(res.isBlocked)
        assertTrue(res.reason.contains("TLD"))
    }

    @Test
    fun testLevel1UrlMatching_ExplicitSearchTerm_Blocked() {
        val res = Level1UrlFilter.checkUrlOrDomain("https://www.google.com/search?q=free+porn+videos")
        assertTrue(res.isBlocked)
        assertTrue(res.reason.contains("Search Term"))
    }

    @Test
    fun testLevel1UrlMatching_SafeUrl_Allowed() {
        val res = Level1UrlFilter.checkUrlOrDomain("https://www.wikipedia.org/wiki/Kotlin")
        assertFalse(res.isBlocked)
    }

    @Test
    fun testLevel2Database_KnownAdultDomain_Blocked() {
        val l2 = Level2DatabaseFilter()
        val res = l2.isDomainInDatabase("subdomain.xvideos.com")
        assertTrue(res.isBlocked)
    }

    @Test
    fun testLevel2Database_CleanDomain_Allowed() {
        val l2 = Level2DatabaseFilter()
        val res = l2.isDomainInDatabase("github.com")
        assertFalse(res.isBlocked)
    }

    @Test
    fun testLevel3ContentAnalyzer_ExplicitContentScore() {
        val content = Level3ContentAnalyzer.ExtractedWebPageContent(
            url = "https://unknown-site.org/page",
            title = "Exclusive Hardcore Porn & Explicit XXX Video Portal",
            h1ToH6Headers = listOf("Watch Free Erotic Movies"),
            bodyText = "Watch nude camgirl shows, adult video streams and explicit pornographic media online.",
            imageElements = listOf(
                Level3ContentAnalyzer.ImageElementInfo(src = "/media/porn_thumb.jpg", altText = "erotic model", title = "hentai")
            ),
            videoSources = listOf("/streams/sex_video.mp4")
        )

        val analysis = Level3ContentAnalyzer.analyzeContent(content)
        assertTrue(analysis.isAdultContent)
        assertTrue(analysis.score >= 15)
    }

    @Test
    fun testLevel3ContentAnalyzer_CleanContentScore() {
        val content = Level3ContentAnalyzer.ExtractedWebPageContent(
            url = "https://news.org/article",
            title = "Latest Tech Innovations in Android Development",
            h1ToH6Headers = listOf("Building Secure Android Apps with Kotlin"),
            bodyText = "Jetpack Compose and Room Database provide modern tools for Android engineers."
        )

        val analysis = Level3ContentAnalyzer.analyzeContent(content)
        assertFalse(analysis.isAdultContent)
        assertEquals(0, analysis.score)
    }

    @Test
    fun testAdultContentFilterFacade() = runBlocking {
        val eval1 = AdultContentFilter.evaluateUrlOrDomainFast("pornhub.com")
        assertTrue(eval1.isAdult)

        val eval2 = AdultContentFilter.evaluateFullWebContent(
            urlOrDomain = "https://clean-educational-site.org",
            performPreflightHttp = false
        )
        assertFalse(eval2.isAdult)
    }
}
