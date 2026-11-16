package com.rushgames.barrier.filter

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

        val normalizedInput = rawInput.trim()

        // Parse the host before decoding query values; decoding the full URL can corrupt its authority.
        val domain = extractDomain(normalizedInput)

        // 1. TLD Check
        val tld = domain.substringAfterLast('.', "")
        if (ADULT_TLDS.contains(tld)) {
            return Level1Result(true, "Blocked by Adult TLD (.$tld)")
        }

        // 2. Keyword matching in Domain/Subdomain
        for (keyword in ADULT_URL_KEYWORDS) {
            if (containsDomainKeyword(domain, keyword)) {
                return Level1Result(true, "Blocked by URL/Domain keyword ($keyword)")
            }
        }

        // 3. Search Engine Query Inspection (Google, Bing, DuckDuckGo, YouTube, etc.)
        if (isSearchQuery(normalizedInput)) {
            val queryText = extractSearchQueryText(normalizedInput)
            for (term in ADULT_SEARCH_TERMS) {
                if (queryText.contains(term)) {
                    return Level1Result(true, "Blocked by Explicit Search Term ($term)")
                }
            }
        }

        // 4. Inspect path segments only. Arbitrary query parameters are not search queries.
        val decodedPath = extractUrlPath(normalizedInput)
        val pathTokens = decodedPath.lowercase(Locale.ROOT)
            .split(Regex("[^a-z0-9]+"))
            .filter(String::isNotEmpty)
        for (keyword in ADULT_URL_KEYWORDS) {
            if (keyword in pathTokens) {
                return Level1Result(true, "Blocked by URL path pattern ($keyword)")
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

    fun extractDomain(urlOrDomain: String): String =
        WebsiteDomainMatcher.normalizeDomain(urlOrDomain).orEmpty()

    private fun extractUrlPath(input: String): String {
        val uri = runCatching { java.net.URI(if (input.contains("://")) input else "https://$input") }
            .getOrNull()
        return decodeUrl(uri?.rawPath.orEmpty())
    }

    private fun isSearchQuery(url: String): Boolean {
        val host = extractDomain(url)
        return host == "google.com" || host.endsWith(".google.com") ||
            host == "bing.com" || host.endsWith(".bing.com") ||
            host == "duckduckgo.com" || host.endsWith(".duckduckgo.com") ||
            host == "search.yahoo.com" || host.endsWith(".search.yahoo.com") ||
            host == "yandex.com" || host.endsWith(".yandex.com") ||
            host == "youtube.com" || host.endsWith(".youtube.com") ||
            host == "youtu.be" || host.endsWith(".youtu.be")
    }

    private fun containsDomainKeyword(domain: String, keyword: String): Boolean =
        domain.split('.').any { label ->
            label.split(Regex("[^a-z0-9]+")).contains(keyword)
        }

    private fun extractSearchQueryText(url: String): String {
        val queryParameters = url.substringAfter('?', "").substringBefore('#').split('&')
        val searchValue = queryParameters.firstNotNullOfOrNull { parameter ->
            val (name, value) = parameter.split('=', limit = 2).let {
                it.first() to it.getOrElse(1) { "" }
            }
            value.takeIf { name.equals("q", ignoreCase = true) ||
                name.equals("query", ignoreCase = true) ||
                name.equals("search_query", ignoreCase = true) }
        }.orEmpty()

        return decodeUrl(searchValue.replace('+', ' ')).lowercase(Locale.ROOT)
    }
}
