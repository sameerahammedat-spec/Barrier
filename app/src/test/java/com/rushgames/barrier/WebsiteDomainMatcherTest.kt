package com.rushgames.barrier

import com.rushgames.barrier.filter.WebsiteDomainMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebsiteDomainMatcherTest {

    @Test
    fun normalizesCaseTrailingDotPortAndInternationalizedDomain() {
        assertEquals("example.com", WebsiteDomainMatcher.normalizeDomain("HTTPS://Example.COM.:443/path"))
        assertEquals("xn--maana-pta.com", WebsiteDomainMatcher.normalizeDomain("mañana.com"))
    }

    @Test
    fun parsesHostRatherThanUserInfoOrPath() {
        assertEquals(
            "example.com",
            WebsiteDomainMatcher.normalizeDomain("https://user:password@example.com:8443/path")
        )
    }

    @Test
    fun matchesExactHostAndSubdomainsAtLabelBoundary() {
        assertTrue(WebsiteDomainMatcher.matchesDomain("example.com", "example.com"))
        assertTrue(WebsiteDomainMatcher.matchesDomain("www.example.com", "example.com"))
        assertTrue(WebsiteDomainMatcher.matchesDomain("shop.eu.example.com", "example.com"))
        assertFalse(WebsiteDomainMatcher.matchesDomain("notexample.com", "example.com"))
        assertFalse(WebsiteDomainMatcher.matchesDomain("example.com.attacker.test", "example.com"))
    }

    @Test
    fun malformedOrEmptyRulesDoNotMatch() {
        assertEquals(null, WebsiteDomainMatcher.normalizeDomain(""))
        assertEquals(null, WebsiteDomainMatcher.normalizeDomain("https:///path"))
        assertFalse(WebsiteDomainMatcher.matchesDomain("example.com", "bad rule"))
    }
}
