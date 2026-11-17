package com.rushgames.barrier.filter

import java.net.IDN
import java.net.URI
import java.util.Locale

/**
 * Canonical host parsing and boundary-aware domain matching shared by blocking paths.
 *
 * A rule matches its exact host and true subdomains only; for example, example.com matches
 * www.example.com but never notexample.com.
 */
object WebsiteDomainMatcher {

    fun normalizeDomain(input: String): String? {
        val value = input.trim()
        if (value.isEmpty()) return null

        val uri = runCatching {
            URI(if (value.contains("://")) value else "https://$value")
        }.getOrNull() ?: return null

        val authority = uri.rawAuthority ?: return null
        val authorityWithoutUserInfo = authority.substringAfterLast('@')
        val rawHost = uri.host ?: authorityWithoutUserInfo.substringBeforeLastPort() ?: return null
        val host = rawHost.removeSuffix(".")
        if (host.isEmpty() || host.startsWith("[") || host.endsWith("]")) return null

        val asciiHost = runCatching {
            IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT)
        }.getOrNull() ?: return null
        if (asciiHost.length > 253) return null

        val labels = asciiHost.split('.')
        if (labels.any { label ->
                label.isEmpty() || label.length > 63 ||
                    label.startsWith('-') || label.endsWith('-') ||
                    label.any { !(it in 'a'..'z' || it in '0'..'9' || it == '-') }
            }) return null

        return asciiHost
    }

    fun matchesDomain(host: String, rule: String): Boolean {
        val normalizedHost = normalizeDomain(host) ?: return false
        val normalizedRule = normalizeDomain(rule) ?: return false
        return normalizedHost == normalizedRule || normalizedHost.endsWith(".$normalizedRule")
    }

    fun matchesAny(host: String, rules: Iterable<String>): Boolean =
        rules.any { matchesDomain(host, it) }

    private fun String.substringBeforeLastPort(): String? {
        val colon = lastIndexOf(':')
        if (colon < 0) return this
        val port = substring(colon + 1)
        if (port.isEmpty() || port.any { !it.isDigit() }) return null
        return substring(0, colon)
    }
}
