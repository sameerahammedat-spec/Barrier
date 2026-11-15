package com.rushgames.barrier.filter

import android.net.Uri
import java.net.URLDecoder
import java.util.Locale

/**
 * Level 1 Adult Content Filter: Fast URL, Domain & Query Parameter Matching.
 * Performs fast pattern matching, TLD checks, query parameter inspection, and decodes obfuscated URLs.
 */
object Level1UrlFilter {

    private val ADULT_TLDS = setOf(
        "porn", "xxx", "adult", "sex", "cam", "erotica", "fetish", "nude", "gay", "lesbian"
    )

    private val ADULT_URL_KEYWORDS = setOf(
        "porn", "pornhub", "xvideos", "xnxx", "xhamster", "redtube", "youporn",
        "chaturbate", "spankbang", "brazzers", "onlyfans", "livejasmin", "stripchat",
        "hentai", "erotic", "nsfw", "camgirl", "sex", "nude", "nudity", "blowjob",
        "cum", "fetish", "hardcore", "milf", "incest", "playboy", "penthouse",
        "eporner", "xbeeg", "tub8", "beeg", "rule34", "gelbooru", "tsnares"
    )

    private val ADULT_SEARCH_TERMS = setOf(
        "porn", "xxx", "sex video", "nude pics", "hardcore sex", "erotic video",
        "naked women", "naked girls", "pornography", "hentai video", "cam show",
        "free porn", "watch sex", "hd porn"
    )

    data class Level1Result(
        val isBlocked: Boolean,
        val reason: String = ""
    )

    fun checkUrlOrDomain(rawInput: String): Level1Result {
        if (rawInput.isBlank()) return Level1Result(false)

        val decodedInput = decodeUrl(rawInput).lowercase(Locale.ROOT).trim()

        // Extract host / domain
        val domain = extractDomain(decodedInput)

        // 1. TLD Check
        val tld = domain.substringAfterLast('.', "")
        if (ADULT_TLDS.contains(tld)) {
            return Level1Result(true, "Blocked by Adult Top-Level Domain (.$tld)")
        }

        // 2. Keyword matching in Domain/Subdomain
        for (keyword in ADULT_URL_KEYWORDS) {
            if (domain.contains(keyword)) {
                return Level1Result(true, "Blocked by URL/Domain keyword ($keyword)")
            }
        }

        // 3. Search Engine Query Inspection (Google, Bing, DuckDuckGo, YouTube, etc.)
        if (isSearchQuery(decodedInput)) {
            val queryText = extractSearchQueryText(decodedInput)
            for (term in ADULT_SEARCH_TERMS) {
                if (queryText.contains(term)) {
                    return Level1Result(true, "Blocked by Explicit Search Term ($term)")
                }
            }
        }

        // 4. Path & Query String Inspection for raw URLs
        if (decodedInput.contains("/") || decodedInput.contains("?")) {
            for (keyword in ADULT_URL_KEYWORDS) {
                if (decodedInput.contains("/$keyword") || decodedInput.contains("=$keyword") || decodedInput.contains("_$keyword")) {
                    return Level1Result(true, "Blocked by URL path/query pattern ($keyword)")
                }
            }
        }

        return Level1Result(false)
    }

    private fun decodeUrl(input: String): String {
        return try {
            var result = input
            if (result.contains("%")) {
                result = URLDecoder.decode(result, "UTF-8")
            }
            result
        } catch (e: Exception) {
            input
        }
    }

    fun extractDomain(urlOrDomain: String): String {
        val clean = urlOrDomain.trim().lowercase(Locale.ROOT)
        val withoutProtocol = when {
            clean.startsWith("http://") -> clean.substring(7)
            clean.startsWith("https://") -> clean.substring(8)
            else -> clean
        }
        val domainPart = withoutProtocol.split('/', '?', '#', ':')[0]
        return domainPart.removePrefix("www.")
    }

    private fun isSearchQuery(url: String): Boolean {
        return url.contains("google.com/search") ||
                url.contains("bing.com/search") ||
                url.contains("duckduckgo.com/") ||
                url.contains("search.yahoo.com/") ||
                url.contains("yandex.com/search") ||
                url.contains("youtube.com/results")
    }

    private fun extractSearchQueryText(url: String): String {
        return try {
            val uri = Uri.parse(url)
            val query = uri.getQueryParameter("q")
                ?: uri.getQueryParameter("query")
                ?: uri.getQueryParameter("search_query")
                ?: ""
            query.lowercase(Locale.ROOT)
        } catch (e: Exception) {
            ""
        }
    }
}
